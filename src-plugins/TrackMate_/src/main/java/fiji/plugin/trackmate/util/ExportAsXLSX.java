package fiji.plugin.trackmate.util;

import ij.ImagePlus;
import ij.ImageStack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Coding of XLSX-Documents belongs to Microsoft
 * 
 */

public class ExportAsXLSX {

	public static void exportImageAsXlsx(ImagePlus img, File targetFile) {
		
		String filepath = targetFile.getAbsolutePath();
		if (!filepath.endsWith(".xlsx")) {
			filepath += ".xlsx";
			targetFile = new File(filepath);
		}
		
		
		ZipOutputStream out=null;
        try {
	        // out put file 
	        out = new ZipOutputStream(new FileOutputStream(targetFile));
	
			write_Content_types(out);
			write_DocProps(out);
			write__rels(out);
			write_xl(out, img);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (out!= null) {
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
	}
	
	
	
	private static void write_Content_types(ZipOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry("[Content_Types].xml")); 
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">".getBytes());
		out.write("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>".getBytes());
		out.write("<Default Extension=\"xml\" ContentType=\"application/xml\"/>".getBytes());
		out.write("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>".getBytes());
		out.write("<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>".getBytes());
		out.write("<Override PartName=\"/xl/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>".getBytes());
		out.write("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>".getBytes());
		out.write("<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>".getBytes());
		out.write("<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>".getBytes());
		out.write("</Types>".getBytes());
	}

	private static void write_DocProps(ZipOutputStream out) throws IOException {
		write_DocProbs_App(out);
		write_DocProbs_Core(out);
	}
	
	private static void write_DocProbs_App(ZipOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry("docProps/app.xml")); 
		
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">".getBytes());
		out.write("<TotalTime>0</TotalTime><Application>Microsoft Excel</Application><DocSecurity>0</DocSecurity><ScaleCrop>false</ScaleCrop>".getBytes());
		out.write("<HeadingPairs><vt:vector size=\"2\" baseType=\"variant\"><vt:variant><vt:lpstr>Arbeitsblätter</vt:lpstr></vt:variant><vt:variant><vt:i4>1</vt:i4></vt:variant></vt:vector></HeadingPairs>".getBytes());
		//TODO Muss Tabelle durch was anderes ersetzt werden?
		out.write("<TitlesOfParts><vt:vector size=\"1\" baseType=\"lpstr\"><vt:lpstr>Tabelle1</vt:lpstr></vt:vector></TitlesOfParts>".getBytes());
		out.write("<LinksUpToDate>false</LinksUpToDate><SharedDoc>false</SharedDoc><HyperlinksChanged>false</HyperlinksChanged><AppVersion>14.0300</AppVersion></Properties>".getBytes());
		
			
			
			
			
			
	}
	
	private static void write_DocProbs_Core(ZipOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry("docProps/core.xml")); 

		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">".getBytes());
		out.write("<dc:creator>Fiji</dc:creator><cp:lastModifiedBy>Fiji</cp:lastModifiedBy>".getBytes());
		
		Calendar rightNow = Calendar.getInstance();

		String year = rightNow.get(Calendar.YEAR)+"";
		String month;
		int intMonth = rightNow.get(Calendar.MONTH)+1;
		if (intMonth<10) {
			month = "0"+intMonth;
		} else {
			month = intMonth+"";
		}
		String day;
		int intDay = rightNow.get(Calendar.DAY_OF_MONTH);
		if (intDay<10) {
			day = "0"+intDay;
		} else {
			day = intDay+"";
		}
		
		String hour;
		int intHour = rightNow.get(Calendar.HOUR_OF_DAY);
		if (intHour<10) {
			hour = "0"+intHour;
		} else {
			hour = intHour+"";
		}
		String min;
		int intMin = rightNow.get(Calendar.MINUTE);
		if (intMin<10) {
			min = "0"+intMin;
		} else {
			min = intMin+"";
		}
		String secound;
		int intSecound = rightNow.get(Calendar.SECOND);
		if (intSecound<10) {
			secound = "0"+intSecound;
		} else {
			secound = intSecound+"";
		}
		
		String time = year+"-"+month+"-"+day+"T"+hour+":"+min+":"+secound+"Z";
		
		out.write("<dcterms:created xsi:type=\"dcterms:W3CDTF\">".getBytes());
		out.write(time.getBytes());
		out.write("</dcterms:created>".getBytes());
		out.write("<dcterms:modified xsi:type=\"dcterms:W3CDTF\">".getBytes());
		out.write(time.getBytes());
		out.write("</dcterms:modified>".getBytes());
		out.write("</cp:coreProperties>".getBytes());
		
		
		
		
	}
	
	private static void write__rels(ZipOutputStream out) throws IOException {
		write_rels(out);
	}
	
	private static void write_rels(ZipOutputStream out) throws IOException {
		
		out.putNextEntry(new ZipEntry("_rels/.rels")); 

		
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>".getBytes());
		
		/*
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">".getBytes());
		out.write("<Relationship Target=\"docProps/app.xml\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Id=\"rId3\"/>".getBytes());
		out.write("<Relationship Target=\"docProps/core.xml\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Id=\"rId2\"/>".getBytes());
		out.write("<Relationship Target=\"xl/workbook.xml\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Id=\"rId1\"/>".getBytes());
		out.write("</Relationships>".getBytes());*/
	}
		
	private static void write_xl(ZipOutputStream out, ImagePlus img) throws IOException {
		write_xl_styles(out);
		write_xl_workbook(out);
		write_xl__rels(out);
		write_xl_theme(out);
		write_xl_worksheets(out, img);
	}
	
	private static void write_xl_styles(ZipOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry("xl/styles.xml")); 
		
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" mc:Ignorable=\"x14ac\" xmlns:x14ac=\"http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac\"><fonts count=\"1\" x14ac:knownFonts=\"1\"><font><sz val=\"11\"/><color theme=\"1\"/><name val=\"Calibri\"/><family val=\"2\"/><scheme val=\"minor\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Standard\" xfId=\"0\" builtinId=\"0\"/></cellStyles><dxfs count=\"0\"/><tableStyles count=\"0\" defaultTableStyle=\"TableStyleMedium2\" defaultPivotStyle=\"PivotStyleLight16\"/><extLst><ext uri=\"{EB79DEF2-80B8-43e5-95BD-54CBDDF9020C}\" xmlns:x14=\"http://schemas.microsoft.com/office/spreadsheetml/2009/9/main\"><x14:slicerStyles defaultSlicerStyle=\"SlicerStyleLight1\"/></ext></extLst></styleSheet>".getBytes());
	}
	
	private static void write_xl_workbook(ZipOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry("xl/workbook.xml")); 
		
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">".getBytes());
		out.write("<fileVersion appName=\"xl\" lastEdited=\"5\" lowestEdited=\"5\" rupBuild=\"9303\"/>".getBytes());
		out.write("<workbookPr defaultThemeVersion=\"124226\"/>".getBytes());
		out.write("<bookViews><workbookView xWindow=\"480\" yWindow=\"75\" windowWidth=\"16515\" windowHeight=\"6465\"/></bookViews>".getBytes());
		out.write("<sheets><sheet name=\"RegionMatrix\" sheetId=\"1\" r:id=\"rId1\"/></sheets>".getBytes());
		out.write("<calcPr calcId=\"145621\"/>".getBytes());
		out.write("</workbook>".getBytes());
				
	}
	
	private static void write_xl__rels(ZipOutputStream out) throws IOException {
		write_xl__rels_workbookXmlRels(out);
	}
	
	private static void write_xl_theme(ZipOutputStream out) throws IOException {
		write_xl_theme_theme1(out);
	}
	
	private static void write_xl_worksheets(ZipOutputStream out, ImagePlus img) throws IOException {
		write_xl_worksheets_sheet1(out, img);
	}
	
	private static void write_xl__rels_workbookXmlRels(ZipOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels")); 
		/*
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>".getBytes());
		*/
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">".getBytes());
		out.write("<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>".getBytes());
		out.write("<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\" Target=\"theme/theme1.xml\"/>".getBytes());
		out.write("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>".getBytes());
		out.write("</Relationships>".getBytes());
	}
	
	private static void write_xl_theme_theme1(ZipOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry("xl/theme/theme1.xml")); 
		
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" name=\"Larissa\"><a:themeElements><a:clrScheme name=\"Larissa\"><a:dk1><a:sysClr val=\"windowText\" lastClr=\"000000\"/></a:dk1><a:lt1><a:sysClr val=\"window\" lastClr=\"FFFFFF\"/></a:lt1><a:dk2><a:srgbClr val=\"1F497D\"/></a:dk2><a:lt2><a:srgbClr val=\"EEECE1\"/></a:lt2><a:accent1><a:srgbClr val=\"4F81BD\"/></a:accent1><a:accent2><a:srgbClr val=\"C0504D\"/></a:accent2><a:accent3><a:srgbClr val=\"9BBB59\"/></a:accent3><a:accent4><a:srgbClr val=\"8064A2\"/></a:accent4><a:accent5><a:srgbClr val=\"4BACC6\"/></a:accent5><a:accent6><a:srgbClr val=\"F79646\"/></a:accent6><a:hlink><a:srgbClr val=\"0000FF\"/></a:hlink><a:folHlink><a:srgbClr val=\"800080\"/></a:folHlink></a:clrScheme><a:fontScheme name=\"Larissa\"><a:majorFont><a:latin typeface=\"Cambria\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/><a:font script=\"Jpan\" typeface=\"ＭＳ Ｐゴシック\"/><a:font script=\"Hang\" typeface=\"맑은 고딕\"/><a:font script=\"Hans\" typeface=\"宋体\"/><a:font script=\"Hant\" typeface=\"新細明體\"/><a:font script=\"Arab\" typeface=\"Times New Roman\"/><a:font script=\"Hebr\" typeface=\"Times New Roman\"/><a:font script=\"Thai\" typeface=\"Tahoma\"/><a:font script=\"Ethi\" typeface=\"Nyala\"/><a:font script=\"Beng\" typeface=\"Vrinda\"/><a:font script=\"Gujr\" typeface=\"Shruti\"/><a:font script=\"Khmr\" typeface=\"MoolBoran\"/><a:font script=\"Knda\" typeface=\"Tunga\"/><a:font script=\"Guru\" typeface=\"Raavi\"/><a:font script=\"Cans\" typeface=\"Euphemia\"/><a:font script=\"Cher\" typeface=\"Plantagenet Cherokee\"/><a:font script=\"Yiii\" typeface=\"Microsoft Yi Baiti\"/><a:font script=\"Tibt\" typeface=\"Microsoft Himalaya\"/><a:font script=\"Thaa\" typeface=\"MV Boli\"/><a:font script=\"Deva\" typeface=\"Mangal\"/><a:font script=\"Telu\" typeface=\"Gautami\"/><a:font script=\"Taml\" typeface=\"Latha\"/><a:font script=\"Syrc\" typeface=\"Estrangelo Edessa\"/><a:font script=\"Orya\" typeface=\"Kalinga\"/><a:font script=\"Mlym\" typeface=\"Kartika\"/><a:font script=\"Laoo\" typeface=\"DokChampa\"/><a:font script=\"Sinh\" typeface=\"Iskoola Pota\"/><a:font script=\"Mong\" typeface=\"Mongolian Baiti\"/><a:font script=\"Viet\" typeface=\"Times New Roman\"/><a:font script=\"Uigh\" typeface=\"Microsoft Uighur\"/><a:font script=\"Geor\" typeface=\"Sylfaen\"/></a:majorFont><a:minorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/><a:font script=\"Jpan\" typeface=\"ＭＳ Ｐゴシック\"/><a:font script=\"Hang\" typeface=\"맑은 고딕\"/><a:font script=\"Hans\" typeface=\"宋体\"/><a:font script=\"Hant\" typeface=\"新細明體\"/><a:font script=\"Arab\" typeface=\"Arial\"/><a:font script=\"Hebr\" typeface=\"Arial\"/><a:font script=\"Thai\" typeface=\"Tahoma\"/><a:font script=\"Ethi\" typeface=\"Nyala\"/><a:font script=\"Beng\" typeface=\"Vrinda\"/><a:font script=\"Gujr\" typeface=\"Shruti\"/><a:font script=\"Khmr\" typeface=\"DaunPenh\"/><a:font script=\"Knda\" typeface=\"Tunga\"/><a:font script=\"Guru\" typeface=\"Raavi\"/><a:font script=\"Cans\" typeface=\"Euphemia\"/><a:font script=\"Cher\" typeface=\"Plantagenet Cherokee\"/><a:font script=\"Yiii\" typeface=\"Microsoft Yi Baiti\"/><a:font script=\"Tibt\" typeface=\"Microsoft Himalaya\"/><a:font script=\"Thaa\" typeface=\"MV Boli\"/><a:font script=\"Deva\" typeface=\"Mangal\"/><a:font script=\"Telu\" typeface=\"Gautami\"/><a:font script=\"Taml\" typeface=\"Latha\"/><a:font script=\"Syrc\" typeface=\"Estrangelo Edessa\"/><a:font script=\"Orya\" typeface=\"Kalinga\"/><a:font script=\"Mlym\" typeface=\"Kartika\"/><a:font script=\"Laoo\" typeface=\"DokChampa\"/><a:font script=\"Sinh\" typeface=\"Iskoola Pota\"/><a:font script=\"Mong\" typeface=\"Mongolian Baiti\"/><a:font script=\"Viet\" typeface=\"Arial\"/><a:font script=\"Uigh\" typeface=\"Microsoft Uighur\"/><a:font script=\"Geor\" typeface=\"Sylfaen\"/></a:minorFont></a:fontScheme><a:fmtScheme name=\"Larissa\"><a:fillStyleLst><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill><a:gradFill rotWithShape=\"1\"><a:gsLst><a:gs pos=\"0\"><a:schemeClr val=\"phClr\"><a:tint val=\"50000\"/><a:satMod val=\"300000\"/></a:schemeClr></a:gs><a:gs pos=\"35000\"><a:schemeClr val=\"phClr\"><a:tint val=\"37000\"/><a:satMod val=\"300000\"/></a:schemeClr></a:gs><a:gs pos=\"100000\"><a:schemeClr val=\"phClr\"><a:tint val=\"15000\"/><a:satMod val=\"350000\"/></a:schemeClr></a:gs></a:gsLst><a:lin ang=\"16200000\" scaled=\"1\"/></a:gradFill><a:gradFill rotWithShape=\"1\"><a:gsLst><a:gs pos=\"0\"><a:schemeClr val=\"phClr\"><a:shade val=\"51000\"/><a:satMod val=\"130000\"/></a:schemeClr></a:gs><a:gs pos=\"80000\"><a:schemeClr val=\"phClr\"><a:shade val=\"93000\"/><a:satMod val=\"130000\"/></a:schemeClr></a:gs><a:gs pos=\"100000\"><a:schemeClr val=\"phClr\"><a:shade val=\"94000\"/><a:satMod val=\"135000\"/></a:schemeClr></a:gs></a:gsLst><a:lin ang=\"16200000\" scaled=\"0\"/></a:gradFill></a:fillStyleLst><a:lnStyleLst><a:ln w=\"9525\" cap=\"flat\" cmpd=\"sng\" algn=\"ctr\"><a:solidFill><a:schemeClr val=\"phClr\"><a:shade val=\"95000\"/><a:satMod val=\"105000\"/></a:schemeClr></a:solidFill><a:prstDash val=\"solid\"/></a:ln><a:ln w=\"25400\" cap=\"flat\" cmpd=\"sng\" algn=\"ctr\"><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill><a:prstDash val=\"solid\"/></a:ln><a:ln w=\"38100\" cap=\"flat\" cmpd=\"sng\" algn=\"ctr\"><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill><a:prstDash val=\"solid\"/></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst><a:outerShdw blurRad=\"40000\" dist=\"20000\" dir=\"5400000\" rotWithShape=\"0\"><a:srgbClr val=\"000000\"><a:alpha val=\"38000\"/></a:srgbClr></a:outerShdw></a:effectLst></a:effectStyle><a:effectStyle><a:effectLst><a:outerShdw blurRad=\"40000\" dist=\"23000\" dir=\"5400000\" rotWithShape=\"0\"><a:srgbClr val=\"000000\"><a:alpha val=\"35000\"/></a:srgbClr></a:outerShdw></a:effectLst></a:effectStyle><a:effectStyle><a:effectLst><a:outerShdw blurRad=\"40000\" dist=\"23000\" dir=\"5400000\" rotWithShape=\"0\"><a:srgbClr val=\"000000\"><a:alpha val=\"35000\"/></a:srgbClr></a:outerShdw></a:effectLst><a:scene3d><a:camera prst=\"orthographicFront\"><a:rot lat=\"0\" lon=\"0\" rev=\"0\"/></a:camera><a:lightRig rig=\"threePt\" dir=\"t\"><a:rot lat=\"0\" lon=\"0\" rev=\"1200000\"/></a:lightRig></a:scene3d><a:sp3d><a:bevelT w=\"63500\" h=\"25400\"/></a:sp3d></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill><a:gradFill rotWithShape=\"1\"><a:gsLst><a:gs pos=\"0\"><a:schemeClr val=\"phClr\"><a:tint val=\"40000\"/><a:satMod val=\"350000\"/></a:schemeClr></a:gs><a:gs pos=\"40000\"><a:schemeClr val=\"phClr\"><a:tint val=\"45000\"/><a:shade val=\"99000\"/><a:satMod val=\"350000\"/></a:schemeClr></a:gs><a:gs pos=\"100000\"><a:schemeClr val=\"phClr\"><a:shade val=\"20000\"/><a:satMod val=\"255000\"/></a:schemeClr></a:gs></a:gsLst><a:path path=\"circle\"><a:fillToRect l=\"50000\" t=\"-80000\" r=\"50000\" b=\"180000\"/></a:path></a:gradFill><a:gradFill rotWithShape=\"1\"><a:gsLst><a:gs pos=\"0\"><a:schemeClr val=\"phClr\"><a:tint val=\"80000\"/><a:satMod val=\"300000\"/></a:schemeClr></a:gs><a:gs pos=\"100000\"><a:schemeClr val=\"phClr\"><a:shade val=\"30000\"/><a:satMod val=\"200000\"/></a:schemeClr></a:gs></a:gsLst><a:path path=\"circle\"><a:fillToRect l=\"50000\" t=\"50000\" r=\"50000\" b=\"50000\"/></a:path></a:gradFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements><a:objectDefaults/><a:extraClrSchemeLst/></a:theme>".getBytes());
	}
	
	private static void write_xl_worksheets_sheet1(ZipOutputStream out, ImagePlus img) throws IOException {
		out.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml")); 
		
		int width = img.getWidth();
		int height = img.getHeight(); 
		ImageStack stack = img.getStack();
		
		if (!(stack.getPixels(1) instanceof int[])) {
			throw new IllegalArgumentException();
		}
		int[] pixels = (int[])(stack.getPixels(1));
		
		
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
		out.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" mc:Ignorable=\"x14ac\" xmlns:x14ac=\"http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac\">".getBytes());
		out.write(("<dimension ref=\"A1:"+getXLSLetter(width)+height+"\"/>").getBytes());
		out.write("<sheetViews><sheetView tabSelected=\"1\" workbookViewId=\"0\"><selection activeCell=\"A1\" sqref=\"A1\"/></sheetView></sheetViews>".getBytes());
		out.write(("<sheetFormatPr baseColWidth=\"3\" defaultRowHeight=\"15\" x14ac:dyDescent=\"0.25\"/>").getBytes());
		
		out.write("<sheetData>".getBytes());
		
		for (int row = 0; row<height; row++) {
			out.write(("<row r=\""+(row+1)+"\" spans=\"1:"+height+"\" x14ac:dyDescent=\"0.25\">").getBytes());
			
			
			for (int col = 0; col<width; col++) {
				out.write(("<c r=\"" + getXLSLetter(col+1)+ (row+1)
						+ "\"><v>" + pixels[row*width+col]
						+ "</v></c>").getBytes());
			}
			out.write("</row>".getBytes());
		}
		out.write("</sheetData>".getBytes());
		out.write("<pageMargins left=\"0.7\" right=\"0.7\" top=\"0.78740157499999996\" bottom=\"0.78740157499999996\" header=\"0.3\" footer=\"0.3\"/>".getBytes());
		out.write("</worksheet>".getBytes());
	}
	
	private static String getXLSLetter(int i) {
		// 26 Letters
		// After Z -> AA
		
		byte[] string;
		
		if (i>789) {
			throw new IllegalArgumentException();
		} else if (i/676>0) {
			// Letter has 3 Positions
			int first = ((i-1)%26)+1;
			int sec = ((((i-first)/26)-1)%26)+1;
			int third = ((i-first-sec*26)/676)%26;
			
			string = new byte[3];
			string[2] = (byte)(first+64);
			string[1] = (byte)(sec+64);
			string[1] = (byte)(third+64);
			
			if (third>24) {
				throw new IllegalArgumentException();
			}
			
		} else if (i/26>0){
			// Letter hast 2 Positions
			int first = ((i-1)%26)+1;
			int sec = ((((i-first)/26)-1)%26)+1;
			
			string = new byte[2];
			string[1] = (byte)(first+64);
			string[0] = (byte)(sec+64);
			
		} else {
			// Letter has only one Position
			int first = ((i-1)%26)+1;
			string = new byte[1];
			string[0] = (byte)(first+64);
		}
		
		return new String(string);
		
	}
}
