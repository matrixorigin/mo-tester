package io.mo.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class YamlUtil {
    private Map info;

    public Map getInfo(String filename) {
        Yaml yaml = new Yaml();
        URL url = YamlUtil.class.getClassLoader().getResource(filename);
        //System.out.println(url);
        if (url != null) {
            try {
                this.info = yaml.load(Files.newInputStream(Paths.get(URLDecoder.decode(url.getFile(), "utf-8"))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return info;
    }
}
