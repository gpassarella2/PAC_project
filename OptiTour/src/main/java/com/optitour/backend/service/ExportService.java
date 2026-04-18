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

/**
 * Service responsabile della generazione del PDF di un itinerario.
 * Implementa l'interfaccia ExportServiceIF.
 */
@Service
public class ExportService implements ExportServiceIF {

    // Repository usato per recuperare i dettagli dei monumenti dal DB
    private final MonumentRepository monumentRepository;

    public ExportService(MonumentRepository monumentRepository) {
        this.monumentRepository = monumentRepository;
    }

    /**
     * Genera un PDF contenente il riepilogo del viaggio.
     * @param trip oggetto Trip con tutte le informazioni dell’itinerario
     * @return array di byte rappresentante il file PDF
     */
    @Override
    public byte[] generateTripPdf(Trip trip) {


        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //dimensione pdf
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            //formato
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(37, 99, 235));
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);

            //intestazione
            Paragraph title = new Paragraph("OPTITOUR", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph(
                "Riepilogo del tuo Itinerario",
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY)
            );
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            document.add(sub);


            document.add(new Paragraph("INFORMAZIONI GENERALI", sectionTitleFont));
            document.add(new Paragraph(" ")); // spazio verticale

            // Tabella a 2 colonne
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            // Conversione metri in km
            double km = trip.getTotalDistanceMeters() != null
                    ? trip.getTotalDistanceMeters() / 1000.0 : 0;

            addInfoCell(infoTable, "Distanza totale:",
                    String.format("%.2f km", km), labelFont, valueFont);

            // Conversione secondi in ore/minuti
            long totalSeconds = trip.getTotalDurationSeconds() != null
                    ? trip.getTotalDurationSeconds() : 0;

            long h = totalSeconds / 3600;
            long m = (totalSeconds % 3600) / 60;

            addInfoCell(infoTable,
                    "Tempo totale stimato (spostamenti+visita):",
                    h + "h " + m + "min", labelFont, valueFont);

            // Numero di tappe
            int numStages = trip.getStages() != null
                    ? trip.getStages().size() : 0;

            addInfoCell(infoTable, "Numero di tappe:",
                    String.valueOf(numStages), labelFont, valueFont);

            // Città del viaggio
            addInfoCell(infoTable, "Città:",
                    trip.getCity(), labelFont, valueFont);
            
            addInfoCell(infoTable, "Punto di partenza:",
                    trip.getStartPoint() != null ? trip.getStartPoint() : "N/D",
                    labelFont, valueFont);
            
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // selezione tappe
            document.add(new Paragraph("ORDINE DELLE TAPPE", sectionTitleFont));
            document.add(new Paragraph(" "));

            // Tabella a 3 colonne: indice, nome monumento, durata visita
            PdfPTable table = new PdfPTable(new float[]{1, 5, 3});
            table.setWidthPercentage(100);

            // Header della tabella
            String[] headers = {"#", "Monumento", "Durata visita"};
            for (String head : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(head, headerFont));
                cell.setBackgroundColor(new Color(37, 99, 235)); // colore header
                cell.setPadding(8);
                table.addCell(cell);
            }

            // tabella
            int count = 1;
            for (TripStage stage : trip.getStages()) {

                // Recupero dati completi del monumento dal database
                Monument mnt = monumentRepository
                        .findById(new ObjectId(stage.getMonumentId()))
                        .orElse(null);

                // Numero progressivo della tappa
                table.addCell(new PdfPCell(
                        new Paragraph(String.valueOf(count++), valueFont)));

                // Nome monumento 
                table.addCell(new PdfPCell(
                        new Paragraph(mnt != null ? mnt.getName() : "N/D", valueFont)));

                // Durata visita associata alla tappa
                table.addCell(new PdfPCell(
                        new Paragraph(stage.getVisitDurationMinutes() + " min", valueFont)));
            }

            document.add(table);

            // Chiusura documento 
            document.close();

        } catch (Exception e) {
            e.printStackTrace(); // gestione errore base 
        }

        // Restituisce il PDF come array di byte
        return out.toByteArray();
    }

    /**
     * Metodo di utilità per aggiungere una riga 
     * nella tabella delle informazioni generali.
     */
    private void addInfoCell(PdfPTable table, String label, String value,
                             Font lFont, Font vFont) {

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