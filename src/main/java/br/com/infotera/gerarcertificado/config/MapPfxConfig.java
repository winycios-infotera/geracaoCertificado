package br.com.infotera.gerarcertificado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MapPfxConfig {

    @Bean(name = "clientPfxMap")
    public Map<String, String> clientPfxMap() {
        Map<String, String> clientPfx = new HashMap<>();

        clientPfx.put("4cantos", "4CantosOperadora.pfx");
        clientPfx.put("aviva rio quente", "avivaRioQuente_2025.pfx");
        clientPfx.put("aviva sauipe", "avivaSauipe_2025.pfx");
        clientPfx.put("aviva valetur", "avivaValetur_2025.pfx");
        clientPfx.put("bancorbras", "bancorbras.pfx");
        clientPfx.put("beachpark", "beachpark.pfx");
        clientPfx.put("bookstay", "bookstay.pfx");
        clientPfx.put("brasileiros por ai", "brasileirosporai.pfx");
        clientPfx.put("brt", "brt.pfx");
        clientPfx.put("bwt", "bwt.pfx");
        clientPfx.put("cativa", "cativa.pfx");
        clientPfx.put("central thermas", "centralthermas.pfx");
        clientPfx.put("clube moms", "clubemoms.pfx");
        clientPfx.put("diversa", "diversa.pfx");
        clientPfx.put("diversa turismo", "diversaturismo.pfx");
        clientPfx.put("ehtl", "ehtl.pfx");
        clientPfx.put("europlus", "europlus.pfx");
        clientPfx.put("fnv", "fnv.pfx");
        clientPfx.put("g7", "g7.pfx");
        clientPfx.put("hresorts", "hresorts.pfx");
        clientPfx.put("incomum bnu", "incomumBNU.pfx");
        clientPfx.put("incomum fln", "incomumFLN.pfx");
        clientPfx.put("itaparica", "itaparica.pfx");
        clientPfx.put("litoral verde", "litoralVerde.pfx");
        clientPfx.put("megatravel", "megatravel.pfx");
        clientPfx.put("mercosur", "mercosur.pfx");
        clientPfx.put("mondiale", "mondiale.pfx");
        clientPfx.put("new age", "newage.pfx");
        clientPfx.put("new it", "newit.pfx");
        clientPfx.put("orinter", "orinter.pfx");
        clientPfx.put("rca", "rca.pfx");
        clientPfx.put("resorts online", "resortsonline.pfx");
        clientPfx.put("schultz", "schultz.pfx");
        clientPfx.put("skyteam", "skyteam.pfx");
        clientPfx.put("soul traveler", "soultraveler.pfx");
        clientPfx.put("tgk", "tgk.pfx");
        clientPfx.put("trielotur", "trielotur.pfx");
        clientPfx.put("viagens promo", "viagenspromo.pfx");
        clientPfx.put("viajar resort", "viajarresorts.pfx");
        clientPfx.put("visual turismo", "visualTurismo.pfx");
        clientPfx.put("vmz viagens", "vmzviagens.pfx");

        return clientPfx;
    }
}