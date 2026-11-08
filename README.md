# Mac's God Gear
Paper plugin source, Java 21, Paper API 1.21.11.
Includes six God Weapons and four God Armor pieces.
Build using GitHub Actions (Actions -> Build God Gear -> artifacts) or `gradle build`.
Install the built JAR in the Paper server `plugins` directory.
Commands: `/godgear give <player> all`, `/godgear give <player> sword`, `/godgear upgrade sword`.
Upgrade requires the matching base item in the main hand.
NOTE: This is an untested source release. Check the server version and test on a backup server before using.
Known limitations: the axe stun is simulated by potion effects; 3x3 mining uses horizontal plane; spear is represented by a trident; double-jump uses Paper's flight toggle and may conflict with flight plugins.
