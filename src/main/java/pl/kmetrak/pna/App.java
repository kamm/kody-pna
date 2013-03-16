package pl.kmetrak.pna;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class App {

	static boolean onlyDownload = false;
	static final Logger logger = Logger.getLogger(App.class);

	public static void main(String[] args) {
		App.run();
	}

	static void run() {
		for (int i = 0; i < 100000; i++) {
			String pna = App.formatAsPNA(i);
			int pageCount = App.parsePageCount(pna);
			if (pageCount == 0) {
				continue;
			} else {
				for (int j = 0; j < pageCount; j++) {
					App.parsePNA(pna, j);
				}
			}
		}
	}

	static String formatAsPNA(int pna) {
		return String.format("%02d-%03d", pna / 1000, pna % 1000);
	}

	static int parsePageCount(String pna) {
		String data = getPage(pna, 0);
		if (data.length() == 0) {//file empty - nothing to parse
			return 0;
		}

		Document doc = Jsoup.parse(data);
		int count = 0;
		Elements links = doc.select("p[align=center] a");
		if (links.size() != 0) {// pagination present
			String lastHref = links.get(links.size() - 1).attr("href");
			String last = lastHref.replaceAll(".*p=([0-9]+).*", "$1");
			count = Integer.parseInt(last) + 1;
		} else if (doc.select("p[align=center]").size() == 1) {//no pagination, but data available
			count = 1;
		} else {//no data found
			count = 0;
		}
		return count;
	}

	static void parsePNA(String pna, int page) {
		String data = getPage(pna, page);
		if (onlyDownload) {//download files to cache, don't parse this (yet)
			return;
		}
		data = data.replaceAll("<td>(.*)<br />", "<td><b>$1</b><br/>"); //dirty hack to fix reading street
		data = data.replace("&nbsp;", " "); //dirty hack to non braking spaces
		Document doc = Jsoup.parse(data);
		Elements records = doc.select("table[width=100%] tr");
		for (int i = 1; i < records.size(); i++) {
			Element record = records.get(i);
			Elements fields = record.select("td");
			String name = fields.get(2).text();
			String city = fields.get(3).text();
			String street = fields.get(4).select("b").text();
			String info = fields.get(4).select("i").text();
			String voivodeship = fields.get(5).text();
			String county = fields.get(6).text();
			String municipality = fields.get(7).text();
			System.out.println(pna + ";" + name + ";" + city + ";" + street + ";" + info + ";" + voivodeship + ";" + county + ";" + municipality);
		}
	}

	static String getPage(String pna, int page) {
		File file = new File(String.format("c:/exp/kody/%s_%d", pna, page)); //TODO: externalize
		file.getParentFile().mkdirs();
		String data = "";
		try {
			if (file.exists()) {
				InputStream is = new FileInputStream(file);
				data = readInputStream(is, "UTF-8");
				if (data.length() == 0) {
					logger.debug("{HIT EMPTY}PNA=" + pna + " PAGE=" + page);
				} else {
					logger.debug("{HIT      }PNA=" + pna + " PAGE=" + page);
				}
				is.close();
			} else {
				data = readURLData("http://kody.poczta-polska.pl/index.php?p=" + page + "&kod=" + pna + "&page=kod");
				PrintStream ps = new PrintStream(file, "UTF-8");
				if (data.contains("Zapytanie nie zwróciło wyników.")) {
					ps.print("");
					data = "";
					logger.debug("{MIS EMPTY}PNA=" + pna + " PAGE=" + page);
				} else {
					ps.print(data);
					logger.debug("{MIS      }PNA=" + pna + " PAGE=" + page);
				}
				ps.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	static String readURLData(String url) {
		String data = "";
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			InputStream is = connection.getInputStream();
			data = readInputStream(is);
			is.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	static String readInputStream(InputStream is) throws IOException {
		String data = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int i = 0;
		while ((i = is.read(buffer)) != -1) {
			baos.write(buffer, 0, i);
		}
		data = baos.toString();
		return data;
	}

	static String readInputStream(InputStream is, String encoding) throws IOException {
		String data = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int i = 0;
		while ((i = is.read(buffer)) != -1) {
			baos.write(buffer, 0, i);
		}
		data = baos.toString(encoding);
		return data;
	}
}
