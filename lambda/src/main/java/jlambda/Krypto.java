package jlambda;

import org.apache.commons.codec.digest.DigestUtils;

public enum Krypto {
    INSTANCE("Krypto"),
    DIGEST("a19bea3b28fc7e9ec586356bf1902d2b");

    private String artifact;

    Krypto(String artifact) {
     this.artifact =artifact;

    }

    public String getartifact() {
        return this.artifact;
    }


    public Boolean authenticateToken(String token) {

        String  md5HexString = DigestUtils.md5Hex(token);
        if(md5HexString.equals(DIGEST.getartifact())) {
            return true;
        }
        return  false;
    }



}
