package io.mo.util;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;

public class YamlUtil {
    private Map info;

    public Map getInfo(String filename) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        URL url = YamlUtil.class.getClassLoader().getResource(filename);
        //System.out.println(url);
        if (url != null) {
            try {
                this.info = (Map) yaml.load(new FileInputStream(URLDecoder.decode(url.getFile(),"utf-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //System.out.println(url.getFile());
        }
        return info;
    }
}
