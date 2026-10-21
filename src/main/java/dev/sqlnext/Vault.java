package dev.sqlnext;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Portable password-encrypted secret store. Master password/key is never persisted. */
public final class Vault {
    private static final int ITERATIONS=600_000;
    private static SecretKey key(char[] password,byte[] salt)throws Exception {
        if(password.length<12)throw new IllegalArgumentException("Use a master password of at least 12 characters.");
        PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);
        try{return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES");}finally{spec.clearPassword();}
    }
    public static void save(Path file,char[] password,Map<String,String> secrets)throws Exception {
        byte[] salt=new byte[16],iv=new byte[12];SecureRandom random=new SecureRandom();random.nextBytes(salt);random.nextBytes(iv);
        byte[] plain=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(secrets);
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),new GCMParameterSpec(128,iv));cipher.updateAAD(new byte[]{'S','Q','V',1});
            byte[] encrypted=cipher.doFinal(plain);ByteBuffer out=ByteBuffer.allocate(4+16+12+encrypted.length);out.put(new byte[]{'S','Q','V',1}).put(salt).put(iv).put(encrypted);
            Files.createDirectories(file.toAbsolutePath().getParent());Path temp=Files.createTempFile(file.toAbsolutePath().getParent(),"vault-",".tmp");
            try {try{Files.setPosixFilePermissions(temp,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){}
                Files.write(temp,out.array());try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
            }finally{Files.deleteIfExists(temp);}
        }finally{Arrays.fill(plain,(byte)0);}
    }
    public static Map<String,String> load(Path file,char[] password)throws Exception {
        byte[] bytes;try(var in=Files.newInputStream(file)){bytes=in.readNBytes(1_048_577);}if(bytes.length<48||bytes.length>1_048_576)throw new IllegalArgumentException("Invalid or oversized vault file.");
        ByteBuffer b=ByteBuffer.wrap(bytes);byte[] magic=new byte[4],salt=new byte[16],iv=new byte[12];b.get(magic).get(salt).get(iv);
        if(!Arrays.equals(magic,new byte[]{'S','Q','V',1}))throw new IllegalArgumentException("Unsupported vault format.");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(password,salt),new GCMParameterSpec(128,iv));cipher.updateAAD(magic);
        byte[] plain=cipher.doFinal(Arrays.copyOfRange(bytes,32,bytes.length));
        try {return new com.fasterxml.jackson.databind.ObjectMapper().readValue(plain,new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});}finally{Arrays.fill(plain,(byte)0);}
    }
}
