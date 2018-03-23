package com.github.abrasha.crypto;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

// Run complete.
//
// Benchmark                       Mode  Cnt     Score     Error  Units
// CryptoInit.withAvailableCipher  avgt    9   134.556 ±   6.433  ns/op
// CryptoInit.withNewCipher        avgt    9  6483.068 ± 234.472  ns/op
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CryptoInit {
    
    private static byte[] MESSAGE_TO_ENCRYPT = "catch me if you can".getBytes(UTF_8);
    
    private static class KeyContainer {
        static final Key key;
        
        static {
            try {
                KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(256);
                
                key = new SecretKeySpec(generator.generateKey().getEncoded(), "AES");
            } catch (Exception e) {
                // ignored
                throw new RuntimeException(e);
            }
        }
        
    }
    
    @State(Scope.Benchmark)
    public static class CipherContainer {
        
        private ThreadLocal<Cipher> cipherThreadLocal = new ThreadLocal<>();
        
        private static Cipher initCipher(){
            try {
    
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, KeyContainer.key);
                return cipher;
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        }
        
        public Cipher getCipher(){
            Cipher cached = cipherThreadLocal.get();
            if (cached == null){
                cached = initCipher();
                cipherThreadLocal.set(cached);
            }
            return cached;
        }
    }
    
    @Benchmark
    public void withAvailableCipher(CipherContainer container, Blackhole bh) throws Exception {
        bh.consume(container.getCipher().doFinal(MESSAGE_TO_ENCRYPT));
    }
    
    @Benchmark
    public void withNewCipher(CipherContainer container, Blackhole bh) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, KeyContainer.key);
        bh.consume(cipher.doFinal(MESSAGE_TO_ENCRYPT));
    }
    
}
