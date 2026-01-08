package com.example.kaptcha;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/captcha")
@CrossOrigin(origins = "*")
public class CaptchaController {
    
    private final DefaultKaptcha captchaProducer;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    
    public CaptchaController(DefaultKaptcha captchaProducer) {
        this.captchaProducer = captchaProducer;
    }
    
    @GetMapping("/new")
    public Map<String, String> newCaptcha() throws Exception {
        String text = captchaProducer.createText();
        String id = UUID.randomUUID().toString();
        cache.put(id, text);
        
        BufferedImage image = captchaProducer.createImage(text);
        File tempFile = new File("/tmp/" + id + ".png");
        ImageIO.write(image, "png", tempFile);
        
        return Map.of(
            "captchaId", id,
            "imageUrl", "/captcha/image/" + id
        );
    }
    
    @GetMapping(value = "/image/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getImage(@PathVariable String id) throws Exception {
        File imageFile = new File("/tmp/" + id + ".png");
        if (imageFile.exists()) {
            return Files.readAllBytes(imageFile.toPath());
        }
        return new byte[0];
    }
    
    @PostMapping("/verify")
    public Map<String, Boolean> verify(@RequestBody Map<String, String> data) {
        String id = data.get("id");
        String answer = data.get("answer");
        
        boolean ok = false;
        if (cache.containsKey(id)) {
            String correctAnswer = cache.get(id);
            ok = correctAnswer != null && correctAnswer.equalsIgnoreCase(answer);
            if (ok) {
                cache.remove(id);
                try {
                    Files.deleteIfExists(Paths.get("/tmp/" + id + ".png"));
                } catch (Exception e) {}
            }
        }
        
        return Map.of("success", ok);
    }
}