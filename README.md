# Rádió – egyszerű internetrádió Androidra

## Funkciók
- Megnyitáskor azonnal szól a legutóbb hallgatott állomás (első indításkor a Rádió 1).
- Az állomásokat a jobb felső sarokban lévő fogaskerékkel lehet kezelni: új link hozzáadása, szerkesztés, törlés, kiválasztás.
- A főképernyőn egy koppintással válthatsz a mentett állomások között.
- Bezárás után is szól tovább a háttérben. Ilyenkor értesítés és zárolt képernyős vezérlő is megjelenik.
- A telefon hangerőgombjai a rádió hangerejét állítják.
- A **Kikapcsolás** gomb leállítja a rádiót, és bezárja az alkalmazást.
- Ha megszakad a hálózat, magától újrakapcsolódik.

## APK készítése a saját gépeden (Android Studio)
1. Töltsd le és telepítsd az Android Studiót: https://developer.android.com/studio
2. Csomagold ki a `RadioApp.zip` fájlt, majd az Android Studióban válaszd a **File → Open** menüpontot, és nyisd meg a `RadioApp` mappát.
3. Várd meg, amíg a Gradle szinkronizál. Az első alkalommal ez néhány percig is eltarthat, mert letölti a szükséges csomagokat.
4. Válaszd a **Build → Build App Bundle(s) / APK(s) → Build APK(s)** menüpontot.
5. Az elkészült APK helye: `app/build/outputs/apk/debug/app-debug.apk`. Másold át a telefonra, és telepítsd. Ehhez engedélyezni kell az „ismeretlen forrásból” történő telepítést.

Úgy is telepítheted, hogy USB-n csatlakoztatod a telefont (bekapcsolt USB-hibakereséssel), és megnyomod a zöld ▶ gombot.

## APK készítése GitHubbal, telepítés nélkül
Töltsd fel a mappát egy GitHub-tárolóba. Az **Actions** fül alatt automatikusan lefut az „APK fordítás” folyamat. Amikor végzett, a `radio-apk` mellékletből letöltheted az APK-t.

## Megjegyzés a linkekről
Közvetlen stream linket adj meg (pl. `.mp3`, `.aac`, `.m3u8`, vagy egy Icecast/Shoutcast címet). A `.pls` és `.m3u` lejátszási listák nem stream linkek: ezeket nyisd meg egy szövegszerkesztőben, és a bennük lévő `http...` linket másold be.
