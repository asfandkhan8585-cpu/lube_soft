package com.lubesoft.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.lubesoft.model.Invoice;
import com.lubesoft.model.InvoiceItem;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility for printing/exporting invoices to PDF.
 */
public class PrintUtil {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10);

    public static void exportInvoiceToPdf(Invoice invoice, String filePath) throws DocumentException, IOException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Title
        Paragraph title = new Paragraph("LubeSoft - Invoice", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // Invoice details
        document.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), HEADER_FONT));
        document.add(new Paragraph("Date: " + invoice.getCreatedAt(), NORMAL_FONT));
        document.add(new Paragraph("Status: " + invoice.getStatus(), NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Items table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 2, 2, 2});

        addHeaderCell(table, "Description");
        addHeaderCell(table, "Qty");
        addHeaderCell(table, "Unit Price");
        addHeaderCell(table, "Total");

        for (InvoiceItem item : invoice.getItems()) {
            table.addCell(new PdfPCell(new Phrase(item.getDescription(), NORMAL_FONT)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", item.getQty()), NORMAL_FONT)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", item.getUnitPrice()), NORMAL_FONT)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", item.getTotal()), NORMAL_FONT)));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // Totals
        document.add(new Paragraph(String.format("Subtotal: %.2f", invoice.getSubtotal()), NORMAL_FONT));
        document.add(new Paragraph(String.format("Tax: %.2f", invoice.getTax()), NORMAL_FONT));
        document.add(new Paragraph(String.format("Discount: %.2f", invoice.getDiscount()), NORMAL_FONT));
        Paragraph total = new Paragraph(String.format("TOTAL: %.2f", invoice.getTotal()), HEADER_FONT);
        document.add(total);

        document.close();
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }
}
