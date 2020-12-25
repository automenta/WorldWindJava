package netvr;

import gov.nasa.worldwind.geom.LatLon;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class AdaptiveOSMLayerTest {

    @Test
    void test1() {

        AdaptiveOSMLayer l = new AdaptiveOSMLayer();
        l.focus(LatLon.fromDegrees(
            40.75632,-73.98809
            //37.2410, -115.8195
        ), 0.005f);
        Set<String> keys = new HashSet();
        Set<String> vals = new HashSet();
        TreeSet<String> kv = new TreeSet();
        l.meta.forEachKeyValue((long k, Map<String,String> v)->{
            v.forEach((key, value) ->{
               keys.add(key);
               vals.add(value);
               kv.add(key + "=" + value);
            });
        });
//        System.out.println("keys: "); keys.forEach(System.out::println);
//        System.out.println();
//        System.out.println("vals: "); vals.forEach(System.out::println);
        System.out.println("kv: "); kv.forEach(System.out::println);
    }
}