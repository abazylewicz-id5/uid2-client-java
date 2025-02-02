package com.uid2.client;

import java.security.MessageDigest;
import java.util.Base64;

public class InputUtil { //from https://github.com/IABTechLab/uid2-operator/blob/master/src/main/java/com/uid2/operator/service/InputUtil.java
  /**
   * @param phoneNumber a phone number in any format
   * @return whether phoneNumber is normalized, which is a requirement for {@link TokenGenerateInput#fromPhone}
   */
  public static boolean isPhoneNumberNormalized(String phoneNumber) {
    //from https://github.com/IABTechLab/uid2-operator/blob/14de3733e72f72adf1d9af7091dee03ea9cdb5b2/src/main/java/com/uid2/operator/service/InputUtil.java#L80
    final int MIN_PHONENUMBER_DIGITS = 10;
    final int MAX_PHONENUMBER_DIGITS = 15;

    // normalized phoneNumber must follow ITU E.164 standard, see https://www.wikipedia.com/en/E.164
    if (phoneNumber == null || phoneNumber.length() < MIN_PHONENUMBER_DIGITS)
      return false;

    // first character must be '+' sign
    if ('+' != phoneNumber.charAt(0))
      return false;

    // count the digits, return false if non-digit character is found
    int totalDigits = 0;
    for (int i = 1; i < phoneNumber.length(); ++i)
    {
      if (!InputUtil.isAsciiDigit(phoneNumber.charAt(i)))
        return false;
      ++totalDigits;
    }

    return totalDigits >= MIN_PHONENUMBER_DIGITS && totalDigits <= MAX_PHONENUMBER_DIGITS;
  }

  static String normalizeEmailString(String email) {
    final StringBuilder preSb = new StringBuilder();
    final StringBuilder preSbSpecialized = new StringBuilder();
    final StringBuilder sb = new StringBuilder();
    StringBuilder wsBuffer = new StringBuilder();

    EmailParsingState parsingState = EmailParsingState.Starting;

    boolean inExtension = false;

    for (int i = 0; i < email.length(); ++i) {
      final char cGiven = email.charAt(i);
      final char c;

      if (cGiven >= 'A' && cGiven <= 'Z') {
        c = (char) (cGiven + 32);
      } else {
        c = cGiven;
      }

      switch (parsingState) {
        case Starting: {
          if (c == ' ') {
            break;
          }
        }
        case Pre: {
          if (c == '@') {
            parsingState = EmailParsingState.SubDomain;
          } else if (c == '.') {
            preSb.append(c);
          } else if (c == '+') {
            preSb.append(c);
            inExtension = true;
          } else {
            preSb.append(c);
            if (!inExtension) {
              preSbSpecialized.append(c);
            }
          }
          break;
        }
        case SubDomain: {
          if (c == '@') {
            return null;
          }
          if (c == ' ') {
            wsBuffer.append(c);
            break;
          }
          if (wsBuffer.length() > 0) {
            sb.append(wsBuffer);
            wsBuffer = new StringBuilder();
          }
          sb.append(c);
        }
      }
    }
    if (sb.length() == 0) {
      return null;
    }
    final String domainPart = sb.toString();

    final String GMAILDOMAIN = "gmail.com";
    final StringBuilder addressPartToUse;
    if (GMAILDOMAIN.equals(domainPart)) {
      addressPartToUse = preSbSpecialized;
    } else {
      addressPartToUse = preSb;
    }
    if (addressPartToUse.length() == 0) {
      return null;
    }

    return addressPartToUse.append('@').append(domainPart).toString();
  }

  private enum EmailParsingState {
    Starting,
    Pre,
    SubDomain,
  }

  static boolean isAsciiDigit(char d)
  {
    return d >= '0' && d <= '9';
  }

  static byte[] base64ToByteArray(String str) { return Base64.getDecoder().decode(str); }
  static String byteArrayToBase64(byte[] b) { return Base64.getEncoder().encodeToString(b); }


  static String getBase64EncodedHash(String input) {
    return byteArrayToBase64(getSha256Bytes(input));
  }

  static byte[] getSha256Bytes(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(input.getBytes());
      return md.digest();
    } catch (Exception e) {
      throw new Uid2Exception("Trouble Generating SHA256", e);
    }
  }
}


