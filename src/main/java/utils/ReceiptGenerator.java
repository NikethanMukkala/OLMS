package utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import models.Borrow;

import java.io.ByteArrayOutputStream;

public class ReceiptGenerator {

    public static byte[] generateReceiptPdf(Borrow borrow, String logoPath) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();

        try {
            if (logoPath != null) {
                Image logo = Image.getInstance(logoPath);
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            }
        } catch (Exception e) {
            System.err.println("Could not load library logo: " + e.getMessage());
        }

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("Fine Payment Receipt", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
        Paragraph sub = new Paragraph("Online Library Management System", subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(30);
        document.add(sub);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        
        Font cellFontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        addTableRow(table, "Transaction ID / Borrow ID:", String.valueOf(borrow.getBorrowId()), cellFontBold, cellFont);
        addTableRow(table, "Student Username:", borrow.getUserId(), cellFontBold, cellFont);
        addTableRow(table, "Book Title:", borrow.getBookTitle(), cellFontBold, cellFont);
        addTableRow(table, "Fine Amount Paid:", "INR " + borrow.getFineAmount(), cellFontBold, cellFont);
        addTableRow(table, "Payment Method:", borrow.getPaymentMethod() != null ? borrow.getPaymentMethod() : "Unknown", cellFontBold, cellFont);
        addTableRow(table, "Status:", "COMPLETED", cellFontBold, cellFont);

        document.add(table);

        Paragraph footer = new Paragraph("Thank you for paying your dues on time!", subFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(40);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    private static void addTableRow(PdfPTable table, String key, String value, Font boldFont, Font regularFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(key, boldFont));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setPadding(8);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(value, regularFont));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setPadding(8);
        table.addCell(cell2);
    }
}
