package com.example.europapeace.services;

import com.example.europapeace.entities.Regiune;
import com.example.europapeace.entities.Stat;
import com.example.europapeace.repositories.RegiuneRepository;
import com.example.europapeace.repositories.StatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

@Service
public class MapImportService {

    @Autowired
    private RegiuneRepository regiuneRepository;

    @Autowired
    private StatRepository statRepository;

    @Transactional
    public void importaHartaNoua() {
        try {
            // 1. Încărcare fișier SVG
            InputStream is = getClass().getResourceAsStream("/static/europa_regiuni.svg");
            if (is == null) {
                System.err.println("❌ EROARE: Fișierul europa_regiuni.svg nu a fost găsit!");
                return;
            }

            // 2. Cache pentru state
            List<Stat> dbStates = statRepository.findAll();
            Map<String, Stat> cache = new HashMap<>();
            for (Stat s : dbStates) {
                if (s.getPrefix() != null && !s.getPrefix().trim().isEmpty()) {
                    cache.put(s.getPrefix().toUpperCase(), s);
                }
            }

            // 3. Parsare SVG
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("path");

            // 4. Curățare DB
            regiuneRepository.deleteAllInBatch();

            List<Regiune> deSalvat = new ArrayList<>();
            int contorFaraStat = 0;

            for (int i = 0; i < nList.getLength(); i++) {
                Element el = (Element) nList.item(i);
                String d = el.getAttribute("d");
                String id = el.getAttribute("id");

                // --- MODIFICARE AICI ---
                // Citim "data-name", așa cum apare în fișierul tău SVG nou
                String numeRegiune = el.getAttribute("data-name");

                // Dacă data-name lipsește, încercăm "name", apoi "id"
                if (numeRegiune == null || numeRegiune.isEmpty()) {
                    numeRegiune = el.getAttribute("name");
                }
                if (numeRegiune == null || numeRegiune.isEmpty()) {
                    numeRegiune = (id != null && !id.isEmpty()) ? id : "Region_" + i;
                }

                if (d != null && !d.isEmpty()) {
                    Regiune r = new Regiune();
                    r.setNume(numeRegiune); // Acum va salva "Marsa", "Hamrun" etc.
                    r.setFormagrafica(d);
                    r.setEsteindependenta(false);

                    if (id != null && !id.isEmpty()) {
                        String prefixSVG = id.contains("-") ? id.split("-")[0] : id;
                        prefixSVG = prefixSVG.toUpperCase();

                        Stat statGasit = cache.get(prefixSVG);

                        if (statGasit == null && id.startsWith("RS-KM")) {
                            statGasit = cache.get("XK");
                        }

                        if (statGasit != null) {
                            r.setStat(statGasit);
                        } else {
                            contorFaraStat++;
                        }
                    }
                    deSalvat.add(r);
                }
            }

            // 5. Salvare finală
            regiuneRepository.saveAll(deSalvat);

            System.out.println("✅ IMPORT REUȘIT: " + deSalvat.size() + " regiuni salvate cu numele din SVG.");

        } catch (Exception e) {
            System.err.println("❌ EROARE CRITICĂ LA IMPORT:");
            e.printStackTrace();
        }
    }
}