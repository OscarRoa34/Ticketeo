package co.edu.uptc.Ticketeo.purchase.services;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import co.edu.uptc.Ticketeo.purchase.models.Purchase;
import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;

@Service
public class TicketPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter PURCHASE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color BRAND_PURPLE = new Color(139, 92, 246);
    private static final Color SOFT_BG = new Color(249, 251, 255);
    private static final Color BORDER_COLOR = new Color(217, 224, 242);
    private static final Color MUTED_TEXT = new Color(100, 116, 139);

    public String generatePurchaseFilename(Purchase purchase) {
        String eventSlug = slugify(safe(purchase.getEventName()));
        String datePart = purchase.getPurchaseDate() == null ? "sin-fecha" : purchase.getPurchaseDate().toLocalDate().format(DATE_FORMAT);
        return "ticketeo_" + eventSlug + "_compra-" + purchase.getId() + "_" + datePart + ".pdf";
    }

    public String generateSingleTicketFilename(PurchasedTicket ticket) {
        Purchase purchase = ticket.getPurchase();
        String eventSlug = slugify(safe(purchase.getEventName()));
        String typeSlug = slugify(safe(ticket.getTicketTypeName()));
        return "ticketeo_" + eventSlug + "_" + typeSlug + "_boleto-" + ticket.getId() + ".pdf";
    }

    public byte[] generatePurchaseTicketsPdf(Purchase purchase) {
        List<PurchasedTicket> tickets = new ArrayList<>(purchase.getTickets());
        tickets.sort(Comparator.comparing(PurchasedTicket::getId));
        return buildTicketsPdf(purchase, tickets);
    }

    public byte[] generateSingleTicketPdf(PurchasedTicket ticket) {
        Purchase purchase = ticket.getPurchase();
        return buildTicketsPdf(purchase, List.of(ticket));
    }

    private byte[] buildTicketsPdf(Purchase purchase, List<PurchasedTicket> tickets) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A6.rotate(), 18, 18, 18, 18);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font brandFont = new Font(Font.HELVETICA, 17, Font.BOLD, BRAND_PURPLE);
            Font titleFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(31, 34, 64));
            Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(55, 65, 81));
            Font codeFont = new Font(Font.COURIER, 8, Font.NORMAL, new Color(55, 65, 81));
            Font minorFont = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED_TEXT);

            for (int i = 0; i < tickets.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }

                PurchasedTicket ticket = tickets.get(i);

                PdfPTable pageWrapper = new PdfPTable(new float[]{1f});
                pageWrapper.setWidthPercentage(100);

                PdfPCell wrapperCell = new PdfPCell();
                wrapperCell.setBorder(Rectangle.NO_BORDER);
                wrapperCell.setPadding(0f);
                wrapperCell.setMinimumHeight(250f);
                wrapperCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                LineSeparator purpleLine = new LineSeparator(1.6f, 100f, BRAND_PURPLE, Element.ALIGN_LEFT, 0);
                wrapperCell.addElement(purpleLine);

                PdfPTable headerTable = new PdfPTable(new float[]{1f});
                headerTable.setWidthPercentage(100);
                headerTable.setSpacingBefore(8);
                headerTable.setSpacingAfter(6);

                PdfPCell brandCell = new PdfPCell();
                brandCell.setBorder(Rectangle.NO_BORDER);
                brandCell.addElement(new Paragraph("TICKETEO", brandFont));
                brandCell.addElement(new Paragraph("Boleto digital", minorFont));
                headerTable.addCell(brandCell);
                wrapperCell.addElement(headerTable);

                PdfPTable contentTable = new PdfPTable(new float[]{2.2f, 1f});
                contentTable.setWidthPercentage(100);
                contentTable.setSpacingBefore(2f);

                PdfPCell detailsCell = new PdfPCell();
                detailsCell.setBorderColor(BORDER_COLOR);
                detailsCell.setBorderWidth(0.8f);
                detailsCell.setPadding(10f);
                detailsCell.setBackgroundColor(SOFT_BG);
                detailsCell.addElement(new Paragraph("Evento: " + safe(purchase.getEventName()), titleFont));
                detailsCell.addElement(new Paragraph("Fecha del evento: " + safeEventDate(purchase), normalFont));
                detailsCell.addElement(new Paragraph("Tipo de boleto: " + safe(ticket.getTicketTypeName()), normalFont));
                detailsCell.addElement(new Paragraph("Metodo: " + purchase.getPaymentMethod().getLabel(), normalFont));
                detailsCell.addElement(new Paragraph("Emitido: " + safePurchaseDate(purchase), normalFont));
                detailsCell.addElement(new Paragraph("Compra #: " + safeNumber(purchase.getId()), normalFont));
                detailsCell.addElement(new Paragraph("Boleto #: " + safeNumber(ticket.getId()), normalFont));
                detailsCell.addElement(new Paragraph("Codigo: " + ticket.getQrCode(), codeFont));
                contentTable.addCell(detailsCell);

                Image qrImage = Image.getInstance(generateQrPng(ticket.getQrCode()));
                qrImage.scaleToFit(94, 94);
                qrImage.setAlignment(Image.ALIGN_CENTER);

                PdfPCell qrCell = new PdfPCell();
                qrCell.setBorderColor(BRAND_PURPLE);
                qrCell.setBorderWidth(0.8f);
                qrCell.setBackgroundColor(new Color(255, 255, 255));
                qrCell.setPadding(6f);
                qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                qrCell.addElement(qrImage);
                contentTable.addCell(qrCell);

                wrapperCell.addElement(contentTable);

                Paragraph entryNotice = new Paragraph("Conserva este boleto para el ingreso", minorFont);
                entryNotice.setSpacingBefore(8f);
                entryNotice.setSpacingAfter(7f);
                wrapperCell.addElement(entryNotice);

                LineSeparator bottomPurpleLine = new LineSeparator(1.6f, 100f, BRAND_PURPLE, Element.ALIGN_LEFT, 0);
                bottomPurpleLine.setOffset(0f);
                wrapperCell.addElement(bottomPurpleLine);

                pageWrapper.addCell(wrapperCell);
                document.add(pageWrapper);
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | WriterException | java.io.IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF de boletos.", ex);
        }
    }

    private byte[] generateQrPng(String content) throws WriterException, java.io.IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                300,
                300,
                java.util.Map.of(EncodeHintType.MARGIN, 1)
        );

        ByteArrayOutputStream qrOutput = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", qrOutput);
        return qrOutput.toByteArray();
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }

    private String safeEventDate(Purchase purchase) {
        return purchase.getEventDate() == null ? "Por definir" : purchase.getEventDate().toString();
    }

    private String safePurchaseDate(Purchase purchase) {
        return purchase.getPurchaseDate() == null ? "N/A" : purchase.getPurchaseDate().format(PURCHASE_DATE_FORMAT);
    }

    private String safeNumber(Long value) {
        return value == null ? "N/A" : String.valueOf(value);
    }

    private String slugify(String raw) {
        String normalized = Normalizer.normalize(raw == null ? "archivo" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "archivo";
        }
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }
}

