package jlambda.layer;


import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.TimeZone;

/**
 * AWS AMI Font data
 * =============== families
 * courier
 * zapfdingbats
 * symbol
 * helvetica
 * times
 * times-roman
 * =============== fonts
 * times-roman
 * courier-oblique
 * times-bolditalic
 * times-bold
 * symbol
 * courier-boldoblique
 * helvetica-oblique
 * courier-bold
 * helvetica-bold
 * helvetica-boldoblique
 * courier
 * helvetica
 * zapfdingbats
 * times-italic
 */

public enum ReportGenerator {

    INSTANCE("Report Generator"),
    COURIER("courier"),
    COURIER_BOLD ("courier-bold"),
    PATTERN("yyyy-MM-dd  - hh:mm");

    private final String artifact;

    ReportGenerator(String artifact) {
        this.artifact = artifact;
    }

    public String runSimpleReport(String name) {
        byte[]  docBytes = null;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Properties props = System.getProperties();
        bootStrap();

        try {
            Document document = getDocumentLocal(name, outStream);
            addHeader(document, "Lambda Report");
            addSubHeader(document, "Lambda System Attributes");
            addLambdaSystemTable(document, props);
            addSubHeader(document, "Java Class Path");
            addLambdaClassPathTable(document, props.get("java.class.path") );
            document.close();
            docBytes = outStream.toByteArray();
            outStream.close();
            outStream= null;
            props = null;
            document = null;
;
        } catch (IOException e) {
            System.out.println("runSimpleReport IOException" +e.getLocalizedMessage());

        }

        String etag =  S3LambdaClient.INSTANCE.writeToBucket(name, docBytes);

        if(etag == null) {
            return "etag is null";
        }

        return etag;
    }

    private Document getDocumentLocal(String name, OutputStream outStream ) {
        Document document = new Document(PageSize.LETTER, 5f, 5f, 4f, 10f);
        PdfWriter.getInstance(document, outStream);
        document.open();
        document.newPage();
        return document;


    }

    private FileOutputStream debugTest() throws FileNotFoundException {
        Path projectBasePath = Paths.get(System.getProperty("user.home"), "aws_code_commit/aws-infra-as-code");

        File pdfOutputFile = new File(projectBasePath.resolve("test.pdf").toUri());
        FileOutputStream pdfOutStream = new FileOutputStream(pdfOutputFile);
        return pdfOutStream;
    }




    Font headerFont;
    Font tableHeaderFont;
    Font tableDataFont;
    private void bootStrap() {
        // use the existing fonts
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        tableHeaderFont = FontFactory.getFont(COURIER.artifact, 18f, Font.BOLD, headerCol);
        tableDataFont =  FontFactory.getFont(COURIER.artifact, 11f, Font.NORMAL);
        headerFont =  FontFactory.getFont(COURIER_BOLD.artifact, 24f, Font.NORMAL);


    }

    private void  addHeader(Document doc, String text ) throws IOException {
        PdfPTable table = new PdfPTable( new float[]{50f, 50f});
        table.setSpacingBefore(32f);
        table.setWidthPercentage(100f);

        PdfPCell cell = new PdfPCell( new Phrase( "Savagen Consulting", headerFont ) );
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setVerticalAlignment(PdfPCell.TOP);
        table.addCell(cell);

        cell = new PdfPCell( new Phrase( text, headerFont ) );

        cell.setPaddingRight(4f);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        cell.setVerticalAlignment(PdfPCell.TOP);

        table.addCell(cell);
        doc.add(table);

        table = new PdfPTable(new float[]{60f, 40f});
        table.setWidthPercentage(100f);
        addEmptyTableCells(1, table);
        cell = new PdfPCell( new Phrase( DateTimeFormatter.ofPattern(PATTERN.artifact ).format(ZonedDateTime.now())
                , tableDataFont) );
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingRight(6f);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);

        table.addCell(cell);
        doc.add(table);

    }

    Color headerCol = new Color(90, 183, 180 );

    private void addSubHeader(Document doc, String text) {
        PdfPTable table = new PdfPTable(1);
        table.setSpacingBefore(2f);
        table.setWidthPercentage(100f);

        PdfPCell cell = new PdfPCell( new Phrase(text, tableHeaderFont) );
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(cell);

        doc.add(table);
    }

    private void addLambdaClassPathTable(Document document, Object o) {
        String [] splits = o.toString().split(":");
        PdfPTable table = new PdfPTable( 1);
        table.setSpacingBefore(6f);
        table.setWidthPercentage(100f);
        float BORDER_WIDTH= 0.1f;
        Color borderCol = new Color(60, 183, 180 );
        PdfPCell cell;
        for(String key :  splits ) {
            cell = new PdfPCell( new Phrase(key,  tableDataFont));

            cell.setBorderColor(borderCol);
            cell.setBorderWidth(BORDER_WIDTH);
            cell.setBorder(Rectangle.BOTTOM | Rectangle.TOP | Rectangle.LEFT);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        document.add(table);
    }

    private void  addLambdaSystemTable(Document doc, Properties props) {
        PdfPTable table = new PdfPTable( 2);
        table.setSpacingBefore(6f);
        table.setWidthPercentage(100f);
        float BORDER_WIDTH= 0.1f;
        Color borderCol = new Color(60, 183, 180 );
        PdfPCell cell;

        for(Object obj :props.keySet() ) {
            String key = (String)obj;
            if(key.equals("java.class.path")) {
                continue;
            }
            cell = new PdfPCell(
                    new Phrase( key, tableDataFont) );

            cell.setBorderColor(borderCol);
            cell.setBorderWidth(BORDER_WIDTH);
            cell.setBorder(Rectangle.BOTTOM | Rectangle.TOP | Rectangle.LEFT );
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            table.addCell(cell);
            cell = new PdfPCell(
                    new Phrase( props.get(key).toString(), tableDataFont) );

            cell.setBorderColor(borderCol);
            cell.setBorderWidth(BORDER_WIDTH);
            cell.setBorder(Rectangle.BOTTOM | Rectangle.TOP | Rectangle.LEFT );
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            table.addCell(cell);
        }
        doc.add(table);

    }


    private void  addEmptyTableCells(Integer num, PdfPTable table) {
        PdfPCell cell = null;
        for(int k = 0; k < num; k++){
            cell = new PdfPCell(new Phrase(" "));
            cell.setBorder(PdfPCell.NO_BORDER);
            table.addCell(cell);
        }

    }


}
