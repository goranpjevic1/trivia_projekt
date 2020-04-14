import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class Main {

    public static void main(String[] args) throws IOException {
//        S knjiznico Json-simple ustvarimo objekt v katerega bomo vstavljali drzave
        JSONObject obj = new JSONObject();

//        S knjiznico Jsoup prenesemo html datoteko
        Document doc = Jsoup.connect("https://sl.wikipedia.org/wiki/Seznam_glavnih_mest_držav").get();

//        S html datoteke vzamemo tabele za neodvisne drzave in nepriznane ali delno priznane drzave
        Element countries = doc.select("table").get(0);
        Element unrecognizedCountries = doc.select("table").get(1);

//        Dodamo drzave objektu
        addCountries(obj, countries, "countries");
        addCountries(obj, unrecognizedCountries, "unrecognizedCountries");

//        Uporabimo knjiznico Gson, da izpis json objekta boljse formatiramo
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonElement je = JsonParser.parseString(obj.toString());

//        Ustvarimo datoteko v katero bomo zapisali json vsebino
        FileWriter file = new FileWriter("countries.json");
        try {
//            Zapišemo v datoteko
            file.write(gson.toJson(je));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            Zapišemo podatke iz buffera v datoteko
            file.flush();
//            Zapremo datoteko
            file.close();
        }
    }

//    Funkcija, ki doda drzave v json objekt
    private static void addCountries(JSONObject obj, Element countries, String propertyName) {
//        Ustvarimo json array v katerega bomo dali json objekte drzav
        JSONArray countriesArray = new JSONArray();
//        Vse vrstice iz tabele damo v spremenljivko rows
        Elements rows = countries.select("tr");
        for (int i = 1; i < rows.size(); i++) { // Zacnemo z indeksom 1, ker je v indeksu 0 naslovna vrstica tabele
//            Dobimo i-to vrstico
            Element row = rows.get(i);
//            Iz vrstice vzamemo vse celice
            Elements cols = row.select("td");

//            Naredimo json objekt, ki ga bomo dodali v json array
            JSONObject country = new JSONObject();
//            Dodamo ime drzave
            country.put("name", cols.get(0).text());
//            Dodamo glavno mesto
//            Glavna mesta so html povezave (html element a)
            Elements as = cols.get(1).select("a"); // Vzamemo vse povezave iz celice
            if (as.size() == 0) { // Ce ni povezav
//                Vzamemo samo tekst iz celice
                country.put("capitalCity", cols.get(1).text());
            } else {
//                Ce ima drzava vec glavnih mest vzamemo zadnjo
                country.put("capitalCity", as.last().text());
            }

//            Dodamo leto, ko je bilo glavno mesto drzave priznano kot tako
//            Za nekatere drzave tega podatka ni, zato samo dodamo, ce obstaja
            if (cols.size() >= 3) {
//                Z regex dobimo samo zadnjo letnico, brez ostalih podatkov datumov
                Pattern p = Pattern.compile(" ?(\\d+)$");
                Matcher m = p.matcher(cols.get(2).text());
                while (m.find()) {
//                    Uporabimo prvo ujemanje z regexom in z novim regexom odstranimo morebitne presledke
                    country.put("capitalCityDate", m.group(0).replaceAll("\\s+",""));
                }
            }
//            Dodamo drzavo v json array
            countriesArray.add(country);
        }
//        V objekt vseh drzav dodamo json array teh drzav
        obj.put(propertyName, countriesArray);
    }
}
