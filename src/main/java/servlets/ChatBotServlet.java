package servlets;

import com.google.gson.*;
import dao.BookDAO;
import dao.BorrowDAO;
import models.Book;
import models.Borrow;
import utils.OpenAIClient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;

@WebServlet(urlPatterns = "/chatbot", asyncSupported = true)
public class ChatBotServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(ChatBotServlet.class);

    private BookDAO bookDAO;
    private BorrowDAO borrowDAO;
    private OpenAIClient aiClient;
    private OpenAIClient geminiClient;
    private Gson gson;
    
    private String apiKey;
    private String modelName;
    private String baseUrl;
    private String geminiModelName;

    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
        borrowDAO = new BorrowDAO();
        gson = new GsonBuilder().setPrettyPrinting().create();

        // Load config
        apiKey = System.getenv("GROQ_API_KEY");
        modelName = System.getenv("GROQ_MODEL");
        baseUrl = System.getenv("GROQ_BASE_URL");

        String geminiApiKey = System.getenv("GEMINI_API_KEY");
        geminiModelName = System.getenv("GEMINI_MODEL");

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                if (apiKey == null) apiKey = props.getProperty("groq.api.key", "");
                if (modelName == null) modelName = props.getProperty("groq.model", "llama-3.1-8b-instant");
                if (baseUrl == null) baseUrl = props.getProperty("groq.base_url", "https://api.groq.com/openai/v1/chat/completions");
                if (geminiApiKey == null) geminiApiKey = props.getProperty("gemini.api.key", "");
            }
        } catch (Exception e) {
            logger.error("Failed to load config", e);
        }

        if (modelName == null) modelName = "llama-3.1-8b-instant";
        if (baseUrl == null) baseUrl = "https://api.groq.com/openai/v1/chat/completions";
        if (geminiModelName == null) geminiModelName = "gemini-1.5-flash";

        aiClient = new OpenAIClient(apiKey, baseUrl);
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            geminiClient = new OpenAIClient(geminiApiKey, "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        final AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(120000); // 2 minute timeout for agent reasoning

        CompletableFuture.runAsync(() -> {
            try {
                HttpServletRequest req = (HttpServletRequest) asyncContext.getRequest();
                HttpServletResponse res = (HttpServletResponse) asyncContext.getResponse();
                res.setContentType("application/json;charset=UTF-8");
                PrintWriter out = res.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            out.print("{\"reply\":\"Please log in to use LibraryBot.\"}");
            return;
        }

        String userId = (String) session.getAttribute("user_id");

        String body = req.getReader().lines().collect(Collectors.joining());
        String userMessage = "";
        try {
            JsonElement parsedBody = JsonParser.parseString(body);
            if (!parsedBody.isJsonObject()) {
                out.print("{\"reply\":\"Invalid request format. Expected JSON object.\"}");
                return;
            }
            JsonObject reqJson = parsedBody.getAsJsonObject();
            if (reqJson.has("message") && !reqJson.get("message").isJsonNull()) {
                userMessage = reqJson.get("message").getAsString();
                // Prevent massive prompt-injection or context bloat
                if (userMessage.length() > 500) {
                    userMessage = userMessage.substring(0, 500);
                }
                userMessage = utils.Sanitize.html(userMessage);
            } else {
                out.print("{\"reply\":\"Message field missing in request.\"}");
                return;
            }
        } catch (Exception e) {
            out.print("{\"reply\":\"Could not parse your message.\"}");
            return;
        }

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_KEY_HERE")) {
            out.print("{\"reply\":\"⚙️ AI API key is not configured. Please add the GROQ_API_KEY environment variable.\"}");
            return;
        }

        String reply = runAgentLoop(userMessage, userId);
        
        // DeepSeek R1 models include <think> reasoning tags. 
        // We strip them out so the UI only shows the final conversational answer.
        reply = reply.replaceAll("(?s)<think>.*?</think>", "").trim();
        
        JsonObject resp = new JsonObject();
        resp.addProperty("reply", reply);
        out.print(gson.toJson(resp));

            } catch (Exception e) {
                logger.error("Critical error in async chatbot execution", e);
            } finally {
                asyncContext.complete();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // AGENTIC LOOP (OpenAI spec)
    // ─────────────────────────────────────────────────────────────
    private String runAgentLoop(String userMessage, String userId) {
        try {
            JsonArray messages = new JsonArray();

            String sysPrompt = "You are LibraryBot, a highly capable AI assistant for an Online Library Management System (OLMS). \n" +
                "The current student's user ID is: " + userId + ". \n" +
                "You have access to the following tools to fetch live data BEFORE you answer:\n" +
                "- getMyProfile()  <-- USE THIS IF THEY ASK 'WHAT IS MY NAME', 'WHO AM I', OR 'WHAT IS MY ROLL NUMBER'!\n" +
                "- searchBooks(searchTerm: string)  <-- USE TO SEARCH BY TITLE/AUTHOR. FOR GENERAL SUGGESTIONS, PASS AN EMPTY STRING \"\" TO GET ALL BOOKS AND RECOMMEND 2-3!\n" +
                "- reserveBook(bookId: integer)\n" +
                "- getMyBorrows()  <-- USE THIS TO CHECK HOW MANY BOOKS ARE RESERVED OR BORROWED!\n" +
                "- getMyDues()\n" +
                "- getMyFine()\n" +
                "- returnBook(borrowId: integer)\n" +
                "- getLibraryStats()  <-- USE THIS TO CHECK TOTAL NUMBER OF COPIES OR OVERALL BOOKS COUNT!\n\n" +
                "CRITICAL: YOU ARE FORBIDDEN FROM GUESSING OR INVENTING ANSWERS ABOUT RESERVATIONS, BORROWS, FINES, OR CATALOG ITEMS. YOU MUST USE A TOOL FIRST!\n" +
                "IMPORTANT: If you need to use a tool to fetch data before answering, you MUST output ONLY a JSON block like this:\n" +
                "```json\n" +
                "{\n" +
                "  \"tool_call\": \"<tool_name>\",\n" +
                "  \"arguments\": {\"<arg_name>\": \"<arg_value>\"}\n" +
                "}\n" +
                "```\n" +
                "Stop generating immediately after the JSON block. Wait for the tool result. Do NOT output a conversational response if you use a tool. You can call multiple tools in succession if needed by outputting another tool JSON after receiving the previous result.";

            messages.add(createMessage("system", sysPrompt));
            messages.add(createMessage("user", userMessage));

            int maxIterations = 5;
            for (int i = 0; i < maxIterations; i++) {
                String requestBody = buildOpenAIRequest(messages, modelName);
                String rawResponse = null;
                boolean useFallback = false;

                try {
                    rawResponse = aiClient.post(requestBody);
                    JsonElement parsedRaw = JsonParser.parseString(rawResponse);
                    if (parsedRaw.isJsonObject() && parsedRaw.getAsJsonObject().has("error")) {
                        logger.warn("Primary API Error: " + rawResponse);
                        useFallback = true;
                    }
                } catch (Exception e) {
                    logger.warn("Primary API Failed", e);
                    useFallback = true;
                }

                if (useFallback) {
                    if (geminiClient != null) {
                        logger.info("Falling back to Gemini API...");
                        requestBody = buildOpenAIRequest(messages, geminiModelName);
                        rawResponse = geminiClient.post(requestBody);
                    } else {
                        if (rawResponse == null) return "❌ API Error: Connection failed and no fallback configured.";
                    }
                }

                JsonElement parsedRaw = JsonParser.parseString(rawResponse);
                if (!parsedRaw.isJsonObject()) return "❌ API returned unexpected non-JSON response: " + rawResponse;
                JsonObject aiResp = parsedRaw.getAsJsonObject();

                if (aiResp.has("error")) {
                    JsonElement errEl = aiResp.get("error");
                    if (errEl.isJsonObject()) return "❌ API Error: " + (errEl.getAsJsonObject().has("message") ? errEl.getAsJsonObject().get("message").getAsString() : "Unknown");
                    return "❌ API Error: " + errEl.getAsString();
                }

                String content = extractTextContent(aiResp);
                if (content == null) return "⚠️ API returned empty content.";

                // Look for tool call in the output (markdown JSON block)
                String toolJson = extractToolJson(content);
                if (toolJson != null) {
                    messages.add(createMessage("assistant", "```json\n" + toolJson + "\n```"));

                    try {
                        JsonObject tc = JsonParser.parseString(toolJson).getAsJsonObject();
                        String funcName = null;
                        JsonObject args = new JsonObject();
                        
                        if (tc.has("tool_call")) {
                            funcName = tc.get("tool_call").getAsString();
                            if (tc.has("arguments") && tc.get("arguments").isJsonObject()) args = tc.getAsJsonObject("arguments");
                        } else {
                            for (String key : tc.keySet()) {
                                if (key.matches("searchBooks|reserveBook|getMyBorrows|getMyDues|getMyFine|returnBook|getMyProfile|getLibraryStats")) {
                                    funcName = key;
                                    if (tc.get(key).isJsonObject()) {
                                        args = tc.getAsJsonObject(key);
                                    } else if (tc.get(key).isJsonPrimitive()) {
                                        if (key.equals("searchBooks")) args.addProperty("searchTerm", tc.get(key).getAsString());
                                        else if (key.equals("reserveBook")) args.addProperty("bookId", tc.get(key).getAsInt());
                                        else if (key.equals("returnBook")) args.addProperty("borrowId", tc.get(key).getAsInt());
                                    }
                                    break;
                                }
                            }
                        }

                        if (funcName != null) {
                            JsonObject toolResult = executeFunction(funcName, args, userId);
                            messages.add(createMessage("user", "Tool Execution Result:\n```json\n" + gson.toJson(toolResult) + "\n```\nIf you have the data you need, provide the final conversational answer. Otherwise, output another tool JSON."));
                        } else {
                            messages.add(createMessage("user", "Tool execution failed: unrecognized function name. Please output your final answer."));
                        }
                    } catch (Exception e) {
                        messages.add(createMessage("user", "Tool execution failed: invalid JSON arguments. Please output your final answer."));
                    }
                } else {
                    // No tools called, regular text answer returned
                    return content;
                }
            }
            return "❌ Agent reached maximum iterations and gave up.";
        } catch (Exception e) {
            logger.error("LibraryBot Agent Loop Error", e);
            return "❌ LibraryBot encountered an error: " + e.getMessage();
        }
    }

    private JsonObject createMessage(String role, String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", role);
        obj.addProperty("content", content);
        return obj;
    }

    private String extractTextContent(JsonObject openaiResp) {
        try {
            if (!openaiResp.has("choices") || !openaiResp.get("choices").isJsonArray()) return null;
            JsonElement choiceEl = openaiResp.getAsJsonArray("choices").get(0);
            if (!choiceEl.isJsonObject()) return null;
            JsonObject choiceObj = choiceEl.getAsJsonObject();
            
            if (!choiceObj.has("message") || !choiceObj.get("message").isJsonObject()) return null;
            JsonObject messageObj = choiceObj.getAsJsonObject("message");
            if (messageObj.has("content") && !messageObj.get("content").isJsonNull()) {
                return messageObj.get("content").getAsString().trim();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractToolJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1).trim();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // BUILD OPENAI REQUEST
    // ─────────────────────────────────────────────────────────────
    private String buildOpenAIRequest(JsonArray messages, String targetModel) {
        JsonObject root = new JsonObject();
        root.addProperty("model", targetModel);
        root.addProperty("temperature", 0.6);
        root.addProperty("top_p", 0.7);
        root.addProperty("max_tokens", 1024);
        root.add("messages", messages);
        return gson.toJson(root);
    }

    // ─────────────────────────────────────────────────────────────
    // TOOL EXECUTION (Same DAO logic)
    // ─────────────────────────────────────────────────────────────
    private JsonObject executeFunction(String name, JsonObject args, String userId) {
        JsonObject result = new JsonObject();
        try {
            switch (name) {
                case "searchBooks": {
                    String term = args.has("searchTerm") ? args.get("searchTerm").getAsString() : "";
                    List<Book> books = bookDAO.searchBooks(term, null);
                    JsonArray arr = new JsonArray();
                    for (Book b : books) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("bookId", b.getBookId());
                        obj.addProperty("title", b.getTitle());
                        obj.addProperty("author", b.getAuthor());
                        obj.addProperty("category", b.getCategory());
                        obj.addProperty("copiesAvailable", b.getCopiesAvailable());
                        arr.add(obj);
                    }
                    result.addProperty("count", books.size());
                    result.add("books", arr);
                    break;
                }
                case "reserveBook": {
                    int bookId = args.get("bookId").getAsInt();
                    int count = borrowDAO.getBorrowCountByUser(userId);
                    if (count >= 3) {
                        result.addProperty("success", false);
                        result.addProperty("reason", "Limit reached (max 3). Return a book first.");
                        break;
                    }
                    Book book = bookDAO.getBookById(bookId);
                    if (book == null || book.getCopiesAvailable() <= 0) {
                        result.addProperty("success", false);
                        result.addProperty("reason", "Book unavailable or not found.");
                        break;
                    }
                    boolean updated = bookDAO.updateCopies(bookId, -1);
                    if (updated) {
                        Borrow borrow = new Borrow();
                        borrow.setUserId(userId);
                        borrow.setBookId(bookId);
                        borrow.setIssueDate(Date.valueOf(LocalDate.now()));
                        borrow.setDueDate(Date.valueOf(LocalDate.now().plusDays(14)));
                        borrowDAO.insertBorrow(borrow);
                        
                        result.addProperty("success", true);
                        result.addProperty("title", book.getTitle());
                        result.addProperty("dueDate", borrow.getDueDate().toString());
                    } else {
                        result.addProperty("success", false);
                    }
                    break;
                }
                case "getMyBorrows": {
                    List<Borrow> borrows = borrowDAO.getBorrowsByUser(userId, false);
                    JsonArray arr = new JsonArray();
                    long now = System.currentTimeMillis();
                    for (Borrow b : borrows) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("borrowId", b.getBorrowId());
                        obj.addProperty("title", b.getBookTitle());
                        obj.addProperty("dueDate", b.getDueDate().toString());
                        long diff = b.getDueDate().getTime() - now;
                        obj.addProperty("daysRemaining", (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24)));
                        arr.add(obj);
                    }
                    result.addProperty("count", borrows.size());
                    result.add("borrows", arr);
                    break;
                }
                case "getMyDues": {
                    List<Borrow> overdue = borrowDAO.getBorrowsByUser(userId, true);
                    JsonArray arr = new JsonArray();
                    long now = System.currentTimeMillis();
                    for (Borrow b : overdue) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("borrowId", b.getBorrowId());
                        obj.addProperty("title", b.getBookTitle());
                        long days = (long) Math.ceil((now - b.getDueDate().getTime()) / 86400000.0);
                        obj.addProperty("overdueDays", days);
                        obj.addProperty("fineAmount", days * 10.0);
                        arr.add(obj);
                    }
                    result.addProperty("count", overdue.size());
                    result.add("overdueBooks", arr);
                    break;
                }
                case "getMyFine": {
                    result.addProperty("totalFine", borrowDAO.getFineForUser(userId));
                    break;
                }
                case "returnBook": {
                    int borrowId = args.get("borrowId").getAsInt();
                    boolean success = borrowDAO.returnBook(borrowId, userId);
                    result.addProperty("success", success);
                    result.addProperty("borrowId", borrowId);
                    if (!success) result.addProperty("reason", "No match found");
                    break;
                }
                case "getLibraryStats": {
                    result.addProperty("totalCopiesCount", bookDAO.getTotalBooksCount());
                    result.addProperty("availableCopiesCount", bookDAO.getAvailableBooksCount());
                    result.addProperty("uniqueBookTitlesCount", bookDAO.searchBooks("", null).size());
                    break;
                }
                case "getMyProfile": {
                    String profSql = "SELECT roll_no as roll_number, login_name as name, email FROM users WHERE login_name = ?";
                    try (java.sql.Connection conn = utils.DBConnection.getConnection();
                         java.sql.PreparedStatement stmt = conn.prepareStatement(profSql)) {
                        stmt.setString(1, userId);
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                result.addProperty("roll_number", rs.getString("roll_number"));
                                result.addProperty("name", rs.getString("name"));
                                result.addProperty("email", rs.getString("email"));
                                result.addProperty("role", "student");
                            } else {
                                result.addProperty("error", "Profile not found.");
                            }
                        }
                    } catch (Exception dbEx) {
                        result.addProperty("error", "SQL Execution Failed: " + dbEx.getMessage());
                    }
                    break;
                }
                default:
                    result.addProperty("error", "Unknown function: " + name);
            }
        } catch (Exception e) {
            logger.error("Tool execution failed: {}", name, e);
            result.addProperty("error", e.getMessage());
        }
        return result;
    }
}
