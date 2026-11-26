package co.smartvault.platform;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class AuthenticatedEncryption {
    private final SecretKeySpec key;
    private final SecureRandom random=new SecureRandom();
    public AuthenticatedEncryption(String base64Key){
        byte[] bytes;
        try {
            bytes=Base64.getDecoder().decode(base64Key);
        }
        catch(RuntimeException ex){
            throw new IllegalArgumentException("PII_ENCRYPTION_KEY debe ser Base64",ex);
        }
        if(bytes.length!=32)throw new IllegalArgumentException("PII_ENCRYPTION_KEY debe contener 32 bytes aleatorios");
        key=new SecretKeySpec(bytes,"AES");
    }
    public String encrypt(String raw){
        try {
            byte[] nonce=new byte[12];
            random.nextBytes(nonce);
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
            byte[] data=cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            byte[] all=new byte[nonce.length+data.length];
            System.arraycopy(nonce,0,all,0,nonce.length);
            System.arraycopy(data,0,all,nonce.length,data.length);
            return Base64.getEncoder().encodeToString(all);
        }
        catch(GeneralSecurityException ex){
            throw new IllegalStateException("No se pudo cifrar",ex);
        }
    }
    public String decrypt(String encrypted){
        try {
            byte[] all=Base64.getDecoder().decode(encrypted);
            if(all.length<29)throw new IllegalArgumentException("Dato cifrado inválido");
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(all,0,12)));
            return new String(cipher.doFinal(all,12,all.length-12),StandardCharsets.UTF_8);
        }
        catch(GeneralSecurityException ex){
            throw new IllegalArgumentException("Dato cifrado inválido o clave diferente",ex);
        }
    }
}
