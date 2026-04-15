package com.optitour.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.optitour.backend.model.*;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.service.ExportServiceIF;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.awt.Color;

@Service
public class ExportService implements ExportServiceIF {

    private final MonumentRepository monumentRepository;

    public ExportService(MonumentRepository monumentRepository) {
        this.monumentRepository = monumentRepository;
    }

    @Override
    public byte[] generateTripPdf(Trip trip) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font e Colori
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(37, 99, 235));
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);

            // Intestazione
            Paragraph title = new Paragraph("OPTITOUR", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph sub = new Paragraph("Riepilogo del tuo Itinerario", FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            document.add(sub);

            // Sezione Dati Generali (Info dall'immagine)
            document.add(new Paragraph("INFORMAZIONI GENERALI", sectionTitleFont));
            document.add(new Paragraph(" "));
            
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            
            // Distanza Totale (convertita in km)
            double km = trip.getTotalDistanceMeters() != null ? trip.getTotalDistanceMeters() / 1000.0 : 0;
            addInfoCell(infoTable, "Distanza totale:", String.format("%.2f km", km), labelFont, valueFont);
            
            // Tempo di percorrenza
            long totalSeconds = trip.getTotalDurationSeconds() != null ? trip.getTotalDurationSeconds() : 0;
            long h = totalSeconds / 3600;
            long m = (totalSeconds % 3600) / 60;
            addInfoCell(infoTable, "Tempo totale stimato (spostamenti+visita):", h + "h " + m + "min", labelFont, valueFont);
            
            // Numero tappe
            int numStages = trip.getStages() != null ? trip.getStages().size() : 0;
            addInfoCell(infoTable, "Numero di tappe:", String.valueOf(numStages), labelFont, valueFont);
            addInfoCell(infoTable, "Città:", trip.getCity(), labelFont, valueFont);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // Sezione Tappe (Tabella)
            document.add(new Paragraph("ORDINE DELLE TAPPE", sectionTitleFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{1, 5, 4});
            table.setWidthPercentage(100);

            // Header Tabella
            String[] headers = {"#", "Monumento", "Indirizzo"};
            for (String head : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(head, headerFont));
                cell.setBackgroundColor(new Color(37, 99, 235));
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Popolamento Tappe
            int count = 1;
            for (TripStage stage : trip.getStages()) {
                Monument mnt = monumentRepository.findById(new ObjectId(stage.getMonumentId())).orElse(null);
                
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(count++), valueFont)));
                table.addCell(new PdfPCell(new Paragraph(mnt != null ? mnt.getName() : "N/D", valueFont)));
                table.addCell(new PdfPCell(new Paragraph(mnt != null ? mnt.getAddress() : "-", valueFont)));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell cellL = new PdfPCell(new Paragraph(label, lFont));
        cellL.setBorder(Rectangle.NO_BORDER);
        cellL.setPadding(5);
        table.addCell(cellL);

        PdfPCell cellV = new PdfPCell(new Paragraph(value, vFont));
        cellV.setBorder(Rectangle.NO_BORDER);
        cellV.setPadding(5);
        table.addCell(cellV);
    }
}