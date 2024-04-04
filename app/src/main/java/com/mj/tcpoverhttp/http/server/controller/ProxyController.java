package com.mj.tcpoverhttp.http.server.controller;

import com.mj.tcpoverhttp.http.server.ProxyClient;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ProxyController {
    public static Map<Long, Object[]> map = new HashMap<>();

    @GetMapping("/get")
    public String get(@RequestParam("uniqueId") long uniqueId, @RequestParam("state") int state, HttpServletResponse response) throws IOException {
        //System.out.println(uniqueId);
        if(state==0 || state==3) {
            if (map.containsKey(uniqueId)) {
                ((Thread) map.get(uniqueId)[2]).interrupt();
                ((ProxyClient) map.get(uniqueId)[1]).close();
                map.remove(uniqueId);
            }
        }
        if(state==3)
            return "{}";
        response.setContentType("application/json;charset=utf-8");
        if(state==0) {
            ProxyClient proxyClient = new ProxyClient(uniqueId);
            map.put(uniqueId, new Object[]{0L, proxyClient, new Thread(() -> {
                try {
                    Thread.sleep(1000*60*30);
                } catch (InterruptedException e) {
                    return;
                }
                if (map.containsKey(uniqueId)) {
                    ((ProxyClient) map.get(uniqueId)[1]).close();
                    map.remove(uniqueId);
                }
            })});
            ((Thread) map.get(uniqueId)[2]).start();
            proxyClient.start();
            return "{}";
        } else {
            if (!map.containsKey(uniqueId)) {
                return "{}";
            }
            synchronized (map) {
                ((Thread) map.get(uniqueId)[2]).interrupt();
                Object[] objects = map.get(uniqueId);
                objects[2] = new Thread(() -> {
                    try {
                        Thread.sleep(1000 * 60 * 30);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (map.containsKey(uniqueId)) {
                        ((ProxyClient) map.get(uniqueId)[1]).close();
                        map.remove(uniqueId);
                    }
                });
                map.replace(uniqueId, objects);
                ((Thread) map.get(uniqueId)[2]).start();
            }
            return ((ProxyClient)map.get(uniqueId)[1]).getNeedToSend();
        }
    }

    @RequestMapping("/post")
    public String post(@RequestParam("uniqueId") long uniqueId, @RequestParam("index") long index, @RequestBody byte[] bytes) {
        //System.err.println(uniqueId);
        if(!map.containsKey(uniqueId))
            return "";
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (map) {
                    if (!(map.containsKey(uniqueId) && (long) map.get(uniqueId)[0] != index))
                        break;
                }
            }
            if(!map.containsKey(uniqueId))
                return;
            ((ProxyClient)map.get(uniqueId)[1]).send(bytes);
            synchronized (map) {
                ((Thread) map.get(uniqueId)[2]).interrupt();
                Object[] objects = map.get(uniqueId);
                objects[0] = ((long)objects[0])+1;
                objects[2] = new Thread(() -> {
                    try {
                        Thread.sleep(1000 * 60 * 30);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (map.containsKey(uniqueId)) {
                        ((ProxyClient) map.get(uniqueId)[1]).close();
                        map.remove(uniqueId);
                    }
                });
                map.replace(uniqueId, objects);
                ((Thread) map.get(uniqueId)[2]).start();
            }
        }).start();
        return "";
    }
}
