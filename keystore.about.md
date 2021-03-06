## About Android keystore ##

Android keystore is evolving/improving mechanism from one Android version to other.


### Android keystore usability - unstable storage ###
Keep in mind, that [Android keystore can delete all keys](https://code.google.com/p/android/issues/detail?id=61989)
if you change screen lock type (or update fingerprint/PIN/Password/Gesture).
[It practically means information encrypted using keystore can be lost any time.](https://doridori.github.io/android-security-the-forgetful-keystore/#sthash.gFJfhQs6.dpbs)
Therefore it tends to use Android keystore rather as temporary secure storage.



### Android Keystore - min API ###
[The Keystore](https://developer.android.com/reference/java/security/KeyStore.html) itself has been available since API 1 (restricted to use by VPN and WiFi systems).

The [Android keystore System](https://developer.android.com/training/articles/keystore.html) which is typically
backed by hardware (but not necessarily so) was formally introduced in Android 4.3 /API 18

Because of [stack-based buffer overflow vulnerability](https://threatpost.com/patched-code-execution-bug-affects-most-android-users/106880/)
it is safer to use Android keystore since Android KitKat / API 19.

When you wanna use standard Android security screen (PIN/Password/Gesture/Fingerprint) the safe support is since Android Lollipop / API 21.

Enhanced/Improved security of Keystore is then since Android Marshmallow / API 23.

### Android keystore - enough secured for me? or NOT ? ###

**API 19(KitKat)** - user has to grant DEVICE ADMIN rights for KeystoreCompat library in order make this library working relatively securely with API19.
If user don't grant device admin rights, library can't do the job.

**API 21(Lollipop)** - Since Lollipop is KeystoreCompat using standard Android's ScreenLock.
Force to display ScreenLock is still not defined in the certificate it self,
but user has to handle forcing LockScreen.

**API 23(Marshmallow)** - Since Marshmallow there is ScreenLock force ensured directly in the certificate definition.
Marshmallow targets to [hardware-backed keystore](https://source.android.com/security/keystore/) and bring lots of (look for 23+)
[security options](https://developer.android.com/training/articles/keystore.html).
The way of key-pair generation is completely new. Marshmallow also starts support fingerprint authentication.

**KeystoreCompat library suggests to use Keystore since `Marshmallow`, but supports usage since `KitKat`.**

### List of known Keystore vulnerabilites ###

#### 2016 July - Attacker can modify stored keys  ####

In an academic paper published in July 2016, researchers argue that the particular encryption scheme that KeyStore uses
[fails to protect the integrity of keys](https://threatpost.com/android-keystore-encryption-scheme-broken-researchers-say/119092/)
and could be exploited to allow an attacker to modify stored keys through a forgery attack.
KeyStore, which performs key-specific actions through the OpenSSL library,
allows Android apps to store and generate their own cryptographic keys.
By storing keys in a container, KeyStore makes it more difficult to remove them from the device.
Mohamed Sabt and Jacques Traore, two researchers with the French telecom Orange Labs,
claim the scheme associated with the system is "non-provably secure," and could have "severe consequences."
The two point out in their paper "Breaking Into the KeyStore: A Practical Forgery Attack Against Android KeyStore,"
that it's the hash-then-encrypt (HtE) authenticated encryption (AE) scheme in cipher block chaining mode (CBC)
in KeyStore that fails to guarantee the integrity of keys.

#### 2015 August - OpenSSLX509Certificate ####
The problem is in a single vulnerable class in the Android platform, called OpenSSLX509Certificate,
that the researchers were able to create an exploit for.<br/>
http://www.itworldcanada.com/post/new-android-vulnerability-could-give-attackers-full-privileges <br/>
The good news is that Google has fixed the two OpenSSLX509Certificate instances,
patched Android 5.1 ,5.0, Android M and backported the patch to Android 4.4.

#### 2014 June - stack-based buffer overflow vulnerability ####
A successful exploit would compromise a device completely,
allowing an attacker to execute code of their choosing under the keystore process.<br />
https://threatpost.com/patched-code-execution-bug-affects-most-android-users/106880/ <br/>
This serious code-execution vulnerability in Android 4.3 and earlier was patched in KitKat,
the latest version of the operating system.


### Keystore related articles ###
http://www.androidauthority.com/use-android-keystore-store-passwords-sensitive-information-623779/
https://threatpost.com/android-keystore-encryption-scheme-broken-researchers-say/119092/
https://duo.com/blog/more-than-half-of-android-phones-vulnerable-to-encryption-bypass-attacks
https://doridori.github.io/android-security-the-forgetful-keystore/#sthash.gFJfhQs6.dpbs
https://crackstation.net/hashing-security.htm
https://www.owasp.org/index.php/Hashing_Java

# Similar Keystore libraries

## Android keystore in existing libraries ##
https://github.com/Q42/Qlassified-Android - wrapper using the same under the hood approach as KeystoreCompat library,
but designed rather for saving encrypted data generally.<br/>
_In comparison:_ **KeystoreCompat: is designed rather to simplify work with the secret AND offer valuable functionality for work with the LockScreen (covering all API versions since 19)**(check e.g. [login credentials](https://github.com/kotomisak/security-showcase-android)).
For securing complex data to be stored rather permanently use KeystoreCompat with combination of secured persistence(chek e.g. [encrypting Realm](https://github.com/kotomisak/db-showcase-android))

## Omit the keystore approach ##
https://github.com/scottyab/secure-preferences - you can use encryption based on some phrase and encrypt data directly.
But be careful, this approach force developers handle with another secret (besides the own device secret) and list of
potential vulnerabilities will be always rather uknown than using the Android defaults.

https://github.com/Mauin/RxFingerprint - you can use custom fingerprint handling with RXFingerprint.
It uses also Android Keystore for baking keys.
This completely omit android default security/screen and you can implement your own.
The caveat could be the custom access to Keystore.

