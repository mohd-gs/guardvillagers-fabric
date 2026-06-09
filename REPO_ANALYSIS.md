# Guard Villagers Fabric - تحليل شامل للمستودع

## 📋 معلومات عامة
- **اسم المود**: Guard Villagers
- **المعرف (MODID)**: guardvillagers
- **الإصدار**: 3.0.4
- **منصة التشغيل**: Fabric Loader 0.17.2
- **إصدارة ماين كرافت**: 26.1.0
- **Java**: 25
- **Gradle**: 9.4.0
- **Fabric Loom**: 1.15-SNAPSHOT
- **الحزمة الأساسية**: tallestegg.guardvillagers

---

## 🏗️ الجزء 1: نظام البناء (Build System)

### build.gradle
- Plugin: `net.fabricmc.fabric-loom` version `1.15-SNAPSHOT`
- الاعتماديات:
  - `com.mojang:minecraft:26.1.0`
  - `loom.officialMojangMappings()` (لا حاجة لـ Yarn لأن MC 26.1.x غير مشفر)
  - `net.fabricmc:fabric-loader:0.17.2`
  - `net.fabricmc.fabric-api:fabric-api:26.1.0+0.100.0+26.1.0`
- Java source/target: VERSION_25
- `processResources` يوسّع `${version}` في fabric.mod.json

### settings.gradle
- مستودع Maven الخاص بـ Fabric
- foojay-resolver-convention version 0.10.0

### gradle.properties
- `minecraft_version=26.1.0`
- `loader_version=0.17.2`
- `fabric_version=26.1.0+0.100.0+26.1.0`
- `mod_version=3.0.4`
- `maven_group=tallestegg.guardvillagers`
- `archives_base_name=guardvillagers`

### gradle-wrapper.properties
- Gradle 9.4.0

---

## 📦 الجزء 2: ملفات الموارد (Resources)

### fabric.mod.json
- `id`: guardvillagers
- `version`: ${version} (يُوسّع من processResources)
- نقاط الدخول:
  - `main`: `tallestegg.guardvillagers.GuardVillagers` (ModInitializer)
  - `client`: `tallestegg.guardvillagers.client.GuardClientEvents` (ClientModInitializer)
- Mixins: `guardvillagers.mixins`
- المتطلبات: fabricloader >=0.17.0, minecraft ~26.1.0, java >=25, fabric-api

### guardvillagers.mixins.json
- الحزمة: `tallestegg.guardvillagers.mixins`
- مستوى التوافق: JAVA_25
- الـ Mixins:
  1. `VillagerGoalPackagesMixin` - يضيف سلوكيات الحراس لأهداف القرويين
  2. `DefendVillageGoalGolemMixin` - يعدّل هدف دفاع القرية للغولم
  3. `SinglePoolElementMixin` - ينشر حراس في هياكل القرية

### pack.mcmeta
- `min_format`: 88, `max_format`: 88

### الأصول (Assets):
- **أصوات**: `assets/guardvillagers/sounds.json` + ملفات ogg
- **لغات**: 18 ملف ترجمة (cs_cz, de_de, en_us, es_es, es_mx, fil_ph, fr_fr, it_it, ja_jp, ko_kr, pl_pl, pt_br, ro_ro, ru_ru, tr_tr, uk_ua, vi_vn, zh_cn, zh_tw)
- **نصوص الكيان**: guard.png, guard_steve.png + 12 متغير (desert, jungle, plains, savanna, snow, swamp, taiga × عادي و steve)
- **نصوص واجهة**: container/inventory.png
- **نصوص أزرار**: following/ و patrolling/ (8 صور)
- **بيانات**: loot_table/entities/ (3 ملفات), tags/item/convertible_guard_items.json, advancement/adventure/recruit_guard.json

---

## 🔧 الجزء 3: الفئة الأساسية - GuardVillagers.java

### المسار: `tallestegg.guardvillagers.GuardVillagers`
### النوع: `implements ModInitializer`

**الثوابت**:
- `MODID = "guardvillagers"`
- `LOGGER` - SLF4J Logger

**onInitialize() - تسلسل التهيئة**:
1. تحميل الإعدادات: `GuardConfig.load()`
2. تسجيل الكيانات: `GuardEntityType.register()`
3. تسجيل العناصر: `GuardItems.register()`
4. تسجيل الأصوات: `GuardSounds.register()`
5. تسجيل الإحصائيات: `GuardStats.register()`
6. تسجيل جداول الغنائم: `GuardLootTables.register()`
7. تسجيل مستقبلات الشبكة: `GuardNetworking.registerServerReceivers()`
8. تسجيل خصائص الكيان: `FabricDefaultAttributeRegistry.register(GUARD, Guard.createAttributes())`
9. إضافة بيض التفقيط لتبويب Creative: `ItemGroupEvents.modifyEntriesEvent(SPAWN_EGGS)`
10. تسجيل الأحداث: `UseEntityCallback`, `UseBlockCallback`, `ServerEntityEvents.ENTITY_LOAD`

**الطرق المساعدة**:
- `hotvChecker(Player, Guard)` - يتحقق من إمكانية تفاعل اللاعب مع الحارس (بطل القرية أو سمعة كافية)
- `canFollow(Player)` - يتحقق من إمكانية متابعة الحارس للاعب
- `removeModIdFromVillagerType(String)` - يزيل معرف المود من نوع القروي

---

## 📝 الجزء 4: التسجيل (Registration)

### GuardEntityType.java
- **GUARD** = `EntityType<Guard>` مسجّل في `BuiltInRegistries.ENTITY_TYPE`
- الخصائص: `MobCategory.MISC`, حجم 0.6×1.90, ridingOffset -0.7
- يتم التسجيل عبر static initializer

### GuardItems.java
- **GUARD_SPAWN_EGG** = `SpawnEggItem(GuardEntityType.GUARD, 0x5A3F28, 0x3C89A6, new Item.Settings())`
- **ILLUSIONER_SPAWN_EGG** = `SpawnEggItem(EntityType.ILLUSIONER, 0x7B5E3A, 0x7B8FA6, new Item.Settings())`
- مسجّلان في `BuiltInRegistries.ITEM`

### GuardSounds.java
- 5 أحداث صوتية مسجّلة في `BuiltInRegistries.SOUND_EVENT`:
  1. `GUARD_AMBIENT` - صوت الحارس المحيطي
  2. `GUARD_DEATH` - صوت الموت
  3. `GUARD_HURT` - صوت الأذى
  4. `GUARD_YES` - صوت الموافقة
  5. `GUARD_NO` - صوت الرفض
- **ملاحظة مهمة**: في النسخة الأصلية NeoForge كان يعيد `Holder<SoundEvent>`، الآن يعيد `SoundEvent` مباشرة. هذا يعني أن كل الاستدعاءات `GuardSounds.GUARD_YES.value()` أصبحت `GuardSounds.GUARD_YES`

### GuardStats.java
- **GUARDS_MADE** = `Identifier` مسجّل في `BuiltInRegistries.CUSTOM_STAT`

### GuardVillagerTags.java
- **GUARD_CONVERT** = `TagKey<Item>` - عناصر تحويل القروي لحارس

### GuardLootTables.java
- يسجّل `ARMOR_SLOT` كنوع دالة غنائم في `BuiltInRegistries.LOOT_FUNCTION_TYPE`
- يعرّف `SLOT` كـ `ContextKeySet` مخصص
- **ملاحظة**: يستخدم `BiMap<Identifier, ContextKeySet>` لتسجيل سياقات مخصصة

---

## ⚙️ الجزء 5: نظام الإعدادات - GuardConfig.java

### النظام: Gson JSON
- الملف: `config/guardvillagers.json`
- 3 أقسام: `COMMON`, `CLIENT`, `STARTUP`
- التحميل/الحفظ تلقائي عند التهيئة

### CommonConfig (35 إعداد):
| الإعداد | النوع | الافتراضي | الوصف |
|---------|-------|-----------|-------|
| RaidAnimals | boolean | true | المغيرين يهاجمون الحيوانات في الغارات |
| WitchesVillager | boolean | true | الساحرات يهاجمن القرويين |
| IllagersRunFromPolarBears | boolean | true | المغيرون يهربون من الدببة القطبية |
| AttackAllMobs | boolean | true | الحراس يهاجمون كل الأعداء |
| MobsAttackGuards | boolean | false | جميع الأعداء يهاجمون الحراس |
| MobBlackList | List<String> | [...] | قائمة سوداء للكيانات |
| MobWhiteList | List<String> | [] | قائمة بيضاء للكيانات |
| professionsThatHeal | List<String> | [cleric] | مهن المعالجة |
| professionsThatRepairGolems | List<String> | [armorer, weaponsmith] | مهن إصلاح الغولم |
| professionsThatRepairGuards | List<String> | [weaponsmith, armorer, toolsmith] | مهن إصلاح الحراس |
| maxClericHeal | int | 3 | أقصى عدد معالجات في اليوم |
| maxGolemRepair | int | 3 | أقصى عدد إصلاحات غولم |
| maxVillageRepair | int | 3 | أقصى عدد إصلاحات معدات |
| armorersRepairGuardArmor | boolean | true | دراعون يصلحون دروع الحراس |
| ConvertVillagerIfHaveHOTV | boolean | false | تحويل القروي فقط مع بطل القرية |
| BlacksmithHealing | boolean | true | حدائديون يعالجون الغولم |
| ClericHealing | boolean | true | رهبان يعالجون الحراس |
| VillagersRunFromPolarBears | boolean | true | القرويون يهربون من الدببة |
| convertibleProfessions | List<String> | [nitwit, none] | مهن قابلة للتحويل |
| golemFloat | boolean | false | الغولم يعوم على الماء |
| guardSinkToFightUnderWater | boolean | true | الحراس يغوصون للقتال تحت الماء |
| depthGuardHuntUnderwater | int | 5 | عمق القتال تحت الماء |
| mobsGuardsProtectTargeted | List<String> | [...] | كيانات يحميها الحراس عند استهدافها |
| mobsGuardsProtectHurt | List<String> | [...] | كيانات يحميها الحراس عند أذاها |
| guardCrossbowAttackRadius | double | 8.0 | مدى هجوم القوس المتراكب |
| structuresThatSpawnGuards | List<String> | [...] | هياكل تنشر حراس |
| guardSpawnInVillage | int | 6 | عدد الحراس في القرية |
| convertGuardOnDeath | boolean | true | تحويل الحارس لقروي زومبي عند الموت |
| multiFollow | boolean | true | متابعة جماعية عبر الجرس |
| chanceToDropEquipment | double | 100.0 | فرصة إسقاط المعدات |
| GuardsRunFromPolarBears | boolean | false | الحراس يهربون من الدببة |
| GuardsOpenDoors | boolean | true | الحراس يفتحون الأبواب |
| GuardRaiseShield | boolean | false | الحراس يرفعون الدروع دائماً |
| chanceToBreakEquipment | double | 1.0 | فرصة تلف المعدات |
| guardTeleport | boolean | true | انقلاب الحارس للاحقاق باللاعب |
| GuardFormation | boolean | true | تشكيل فالانكس |
| friendlyFireCheckValue | double | -0.9 | زاوية فحص النيران الصديقة |
| FriendlyFire | boolean | true | تجنب إصابة الحلفاء |
| GuardVillagerHelpRange | double | 50.0 | مدى مساعدة الحراس |
| amountOfHealthRegenerated | double | 1.0 | كمية تجدد الصحة |
| guardArrowsHurtVillagers | boolean | true | سهام الحراس تؤذي القرويين |
| giveGuardStuffHOTV | boolean | false | إعطاء عناصر فقط مع بطل القرية |
| setGuardPatrolHotv | boolean | false | تعيين دورية فقط مع بطل القرية |
| reputationRequirement | int | 15 | حد السمعة المطلوب |
| followHero | boolean | true | متابعة فقط مع بطل القرية |
| reputationRequirementToBeAttacked | int | -100 | حد السمعة للهجوم |
| guardPatrolVillageAi | boolean | false | دورية الحراس (قد تسبب بطء) |
| guardPatrolAroundVillageWorkstations | boolean | true | دورية حول محطات العمل |

### ClientConfig (3 إعدادات):
| الإعداد | النوع | الافتراضي | الوصف |
|---------|-------|-----------|-------|
| GuardSteve | boolean | false | استخدام نموذج ستيف |
| bigHeadBabyVillager | boolean | true | رؤوس كبيرة للأطفال |
| guardInventoryNumbers | boolean | true | أرقام في واجهة الحراس |

### StartUpConfig (3 إعدادات):
| الإعداد | النوع | الافتراضي | الوصف |
|---------|-------|-----------|-------|
| healthModifier | double | 20.0 | صحة الحارس |
| speedModifier | double | 0.5 | سرعة الحارس |
| followRangeModifier | double | 20.0 | مدى المتابعة |

---

## 🔗 الجزء 6: المرفقات - GuardDataAttachments.java

### النظام: ConcurrentHashMap<UUID, T>
يخزن بيانات مرفقة بالقرويين (وليس بالحراس) لأن Fabric ليس لديه AttachmentType مثل NeoForge.

### البيانات المخزنة:
| الخريطة | النوع | الوصف |
|---------|-------|-------|
| TIMES_THROWN_POTION | Map<UUID, Integer> | عدد مرات رمي الجرعة |
| TIMES_HEALED_GOLEM | Map<UUID, Integer> | عدد مرات علاج الغولم |
| TIMES_REPAIRED_GUARD | Map<UUID, Integer> | عدد مرات إصلاح الحارس |
| LAST_REPAIRED_GOLEM | Map<UUID, Long> | آخر وقت إصلاح غولم |
| LAST_THROWN_POTION | Map<UUID, Long> | آخر وقت رمي جرعة |
| LAST_REPAIRED_GUARD | Map<UUID, Long> | آخر وقت إصلاح حارس |

### الطرق: getXxx(uuid), setXxx(uuid, value), incrementXxx(uuid), removeEntity(uuid)

---

## 🎯 الجزء 7: الأحداث - HandlerEvents.java

### الأحداث المسجّلة:
1. **onEntityLoad** (`ServerEntityEvents.ENTITY_LOAD`) - عند تحميل كيان:
   - إضافة أهداف هجوم للمراهقين (Raiders → حيوانات)
   - إضافة أهداف هجوم للحراس (Enemies → Guard)
   - إضافة أهداف فرار (Illagers → PolarBear, Villagers → PolarBear/Witch)
   - تعديل أهداف الغولم (HurtByTargetGoal متسامح مع الحراس)
   - إضافة أهداف للزومبي/المدمر/الساحرة/القطط

2. **onEntityInteract** (`UseEntityCallback`) - عند التفاعل مع كيان:
   - تحويل القروي لحارس باستخدام عنصر من tag GUARD_CONVERT مع الانحناء

3. **onBlockInteract** (`UseBlockCallback`) - عند التفاعل مع كتلة:
   - النقر على الجرس لمتابعة جماعية للحراس

4. **onMobSetTarget** (يُستدعى من Mixin) - عند تغيير هدف كيان:
   - جعل الحراس والغولم يدافعون عن القرويين المستهدفين

### طريقة convertVillager():
- تخلق حارس جديد بنفس موقع القروي
- تنسخ المتغير والاسم والمعدات
- تضيف سمعة إيجابية
- تحذف القروي الأصلي
- تُسجّل الإنجاز والإحصائية

---

## 🛡️ الجزء 8: كيان الحارس - Guard.java (1933 سطر)

### الوصف العام:
- يمتد `PathfinderMob` وينفذ `CrossbowAttackMob`, `RangedAttackMob`, `ReputationEventHandler`, `NeutralMob`
- كيان متعدد الأسلحة (سيف، قوس، قوس متراكب، رمح، درع)
- لديه نظام سمعة (gossip) مثل القرويين
- لديه مخزون من 6 خانات (رأس، صدر، ساقين، قدمين، يد ثانوية، يد رئيسية)
- يتبع اللاعب صاحب بطل القرية
- يتحول لقروي زومبي عند القتل من زومبي
- يتحول لساحرة عند ضرب البرق

### البيانات المتزامنة (SynchedEntityData):
| المعرف | النوع | الوصف |
|--------|-------|-------|
| GUARD_POS | Optional<BlockPos> | موقع الدورية |
| PATROLLING | Boolean | هل في دورية |
| GUARD_VARIANT | String | متغير المظهر (plains, desert, etc.) |
| RUNNING_TO_EAT | Boolean | يركض ليأكل |
| DATA_CHARGING_STATE | Boolean | يشحن القوس المتراكب |
| KICKING | Boolean | يركل |
| FOLLOWING | Boolean | يتبع اللاعب |
| DATA_ANGER_END_TIME | Long | وقت انتهاء الغضب |

### الأبعاد حسب الوضعية (SIZE_BY_POSE):
- SLEEPING, FALL_FLYING, SWIMMING, SPIN_ATTACK, CROUCHING, DYING

### مُعدّلات الخصائص:
- `USE_ITEM_SPEED_PENALTY` - إبطاء عند استخدام العناصر (-0.25 ADD_VALUE)
- `HORSE_SPEED_COMPENSATOR` - تعويض سرعة الفرس (1.5 ADD_MULTIPLIED_TOTAL)

### الطرق الرئيسية:
- `createAttributes()` - يحدد الصحة والسرعة والهجوم والمدى
- `finalizeSpawn()` - يحدد المتغير من البيوم ويجهز المعدات
- `completeUsingItem()` - يُنهي استخدام العنصر (يعالج بالطعام)
- `getItemBySlot()` / `setItemSlot()` - يربط المخزون بفتحات المعدات
- `openGui()` - يفتح واجهة الحارس للاعب (ServerPlayNetworking)
- `die()` - يحول لقروي زومبي إذا قُتل بواسطة زومبي
- `thunderHit()` - يحول لساحرة عند ضرب البرق
- `performRangedAttack()` / `performCrossbowAttack()` - هجوم عن بعد
- `getVariant()` / `setVariant()` - متغير المظهر
- `getOwner()` / `setOwnerId()` - مالك الحارس

### الأهداف الداخلية (Inner Goals):

| الهدف | الأولوية | الوصف |
|-------|----------|-------|
| KickGoal | 0 | ركلة الحارس |
| GuardEatFoodGoal | 0 | أكل الطعام للشفاء |
| RaiseShieldGoal | 0 | رفع الدرع |
| GuardRunToEatGoal | 1 | الركض لأكل الطعام |
| RangedCrossbowAttackPassiveGoal | 3 | هجوم بالقوس المتراكب |
| PassiveMobSpearUseGoal | 3 | هجوم بالرمح |
| GuardBowAttack | 3 | هجوم بالقوس |
| GuardMeleeGoal | 3 | هجوم قريب |
| WalkBackToCheckPointGoal | 3 | العودة لنقطة الدورية |
| FollowHeroGoal | 4 | متابعة بطل القرية |
| MoveBackToVillageGoal | 4 | العودة للقرية |
| GuardInteractDoorGoal | 4 | فتح الأبواب |
| FollowShieldGuards | 6 | تشكيل فالانكس |
| GolemRandomStrollInVillageGoal | 5 | تجول في القرية |
| MoveThroughVillageGoal | 5 | المرور عبر القرية |
| FloatGoal | 10 | الطفو (مع تعديل الغوص) |
| HurtByTargetGoal | 2 | الرد على الهجوم |
| HeroHurtByTargetGoal | 3 | حماية بطل القرية |
| HeroHurtTargetGoal | 3 | مهاجمة من يؤذي بطل القرية |
| DefendVillageGuardGoal | 5 | الدفاع عن القرية |
| ResetUniversalAngerTargetGoal | 8 | إعادة ضبط الغضب |

### GuardGroundPathNavigation (داخلي):
- يمتد `GroundPathNavigation` - للتنقل المخصص

---

## 📦 الجزء 9: الحاوية - GuardContainer.java

- يمتد `AbstractContainerMenu`
- 6 خانات حارس (رأس، صدر، ساقين، قدمين، يد ثانوية، يد رئيسية)
- 36 خانة لاعب (27 مخزون + 9 شريط)
- كل خانة حارس لها فحص `hotvChecker()` للإPlacement والPickup
- فحص نوع المعدات لكل خانة (canEquip)
- `quickMoveStack()` - نقل سريع مع منطق أولوية
- `removed()` - ينظف ويعين `interacting = false`

---

## 🌐 الجزء 10: الشبكات - Networking

### GuardNetworking.java
- يسجّل في `PayloadTypeRegistry`:
  - `playS2C`: GuardOpenInventoryPacket
  - `playC2S`: GuardFollowPacket, GuardSetPatrolPosPacket
- يسجّل مستقبلات الخادم: `ServerPlayNetworking.registerGlobalReceiver()`

### GuardOpenInventoryPacket.java
- **نوع**: S2C (خادم → عميل)
- **بيانات**: id (int), size (int), entityId (int)
- **المعرف**: `guardvillagers:open_inventory`
- **الاستخدام**: يفتح واجهة مخزون الحارس

### GuardFollowPacket.java
- **نوع**: C2S (عميل → خادم)
- **بيانات**: entityId (int)
- **المعرف**: `guardvillagers:following`
- **الاستخدام**: يبدل حالة متابعة الحارس

### GuardSetPatrolPosPacket.java
- **نوع**: C2S (عميل → خادم)
- **بيانات**: entityId (int), pressed (boolean)
- **المعرف**: `guardvillagers:set_patrol_pos`
- **الاستخدام**: يعين/يزيل نقطة الدورية

### FollowingPayloadHandler.java
- معالج فارغ لـ FollowingPayload (غير مستخدم فعلياً)

### GuardPacketHandler.java (العميل)
- `openGuardInventory()` - يعالج فتح واجهة المخزون على العميل
- يتحقق من بيئة العميل ويُنشئ GuardContainer و GuardInventoryScreen

---

## 🎨 الجزء 11: العميل (Client)

### GuardClientEvents.java
- ينفذ `ClientModInitializer`
- يُسجّل:
  - Model layers: GUARD, GUARD_STEVE, GUARD_ARMOR (4 أجزاء), GUARD_STEVE_ARMOR (4 أجزاء)
  - Entity renderer: `GuardRenderer` لـ GUARD
  - مستقبل حزمة العميل: `ClientPlayNetworking.registerGlobalReceiver(GuardOpenInventoryPacket.ID)`

### GuardRenderer.java
- يمتد `HumanoidMobRenderer<Guard, GuardRenderState, HumanoidModel<GuardRenderState>>`
- الطبقات:
  - `GuardVariantLayer` - يعرض متغير المظهر (plains, desert, etc.)
  - `ItemInHandLayer` - عناصر في اليد
  - `HumanoidArmorLayer` - دروع (Guard أو Steve حسب الإعداد)
- `extractRenderState()` - يستخرج حالة الرسم من الكيان
- `identifyArmPoses()` - يحدد وضعية الذراع (vanilla فقط، بدون IClientItemExtensions)

### GuardRenderState.java
- يمتد `HumanoidRenderState`
- حقول إضافية: kickTicks, aggressive, eating, blocking, horizontalSpeedSqr, holdingShootable, showQuiver, showShoulderPads, mainHandEmpty, offHandEmpty, mainHandUseAnimation, offHandUseAnimation, variant

### GuardModel.java
- يمتد `HumanoidModel<GuardRenderState>`
- أجزاء إضافية: nose (أنف), quiver (كنانة), shoulderPad (وسائد كتف)
- `setupAnim()` - حركات مخصصة (ركلة، سلاح مرتفع، أكل)
- حجم النسيج: 128×128

### GuardSteveModel.java
- يمتد `HumanoidModel<GuardRenderState>`
- يستخدم `PlayerModel.createMesh()` كقاعدة
- نفس حركات GuardModel + مزامنة القبعة

### GuardArmorModel.java
- يمتد `HumanoidModel<GuardRenderState>`
- `createArmorMeshSet()` - ينشئ شبكات الدروع المخصصة
- يعدّل شكل الرأس والقبعة

### GuardInventoryScreen.java
- يمتد `AbstractContainerScreen<GuardContainer>`
- عناصر واجهة:
  - زر المتابعة (ImageButton مع WidgetSprites)
  - زر الدورية (ImageButton مع WidgetSprites)
  - عرض قلوب الصحة ودروع الحارس
  - عرض الكيان ثلاثي الأبعاد
- يرسل حزم عبر `ClientPlayNetworking.send()`
- يستخدم `RenderPipelines.GUI_TEXTURED` للرسم

---

## 🧬 الجزء 12: الـ Mixins

### VillagerGoalPackagesMixin.java
- **الهدف**: `VillagerGoalPackages`
- **الحقن**:
  1. `getCorePackage` @RETURN - يضيف RepairGolem, HealGuardAndHero, RepairGuardEquipment
  2. `getMeetPackage` @RETURN - يضيف ShareGossipWithGuard
  3. `getIdlePackage` @RETURN - يضيف InteractWith(Guard), ShareGossipWithGuard

### SinglePoolElementMixin.java
- **الهدف**: `SinglePoolElement`
- **الحقن**: `place` @RETURN - ينشر حراس في الهياكل المحددة

### DefendVillageGoalGolemMixin.java
- **الهدف**: `DefendVillageTargetGoal`
- **الحقن**: `canUse` @HEAD - يعدّل فحص السمعة للغولم

---

## 🧠 الجزء 13: سلوكيات القرويين (Villager AI Tasks)

### VillagerHelp.java (أساسي)
- يمتد `Behavior<Villager>`
- يتحقق من المهنة المسموحة ووقت النشاط الأخير
- `checkIfDayHavePassedFromLastActivity()` - فحص مرور يوم كامل

### HealGuardAndHero.java
- الرهبان يرمون جرعات علاجية للحراس واللاعبين
- يختار جرعة تجدد أو شفاء حسب الصحة
- يستخدم `GuardDataAttachments` لتتبع عدد العلاجات
- فحص خط الرؤية قبل الرمي

### RepairGolem.java
- الحدائديون يصلحون الغولم (< 75% صحة)
- يعطي 15 صحة لكل إصلاح
- يستخدم سكة حديدية كعنصر في اليد
- يستخدم `GuardDataAttachments` لتتبع عدد الإصلاحات

### RepairGuardEquipment.java
- الدروعون يصلحون دروع الحراس
- الأسلحةيون يصلحون أسلحة الحراس
- يقلل قيمة الضرر عشوائياً
- يستخدم `GuardDataAttachments` لتتبع عدد الإصلاحات

### ShareGossipWithGuard.java
- القرويون يتشاركون الإشاعات مع الحراس
- يعطي نصف الطعام الفائض للحارس في اليد الثانوية

---

## 🗡️ الجزء 14: أهداف الذكاء الاصطناعي (AI Goals)

### AttackEntityDaytimeGoal.java
- العناكب تهاجم الحراس نهاراً فقط (نفس هدف العنكبوت الخاص)

### GetOutOfWaterGoal.java
- الكيانات تخرج من الماء (للغولم)
- يمتد `WaterAvoidingRandomStrollGoal`

### GolemFloatWaterGoal.java
- الغولم يعوم على سطح الماء
- يمتد `FloatGoal` مع شرط ارتفاع السائل

---

## 🏆 الجزء 15: جداول الغنائم

### GuardLootTables.java
- يسجّل `ARMOR_SLOT` كنوع دالة غنائم
- يعرّف `SLOT` كسياق مخصص يتطلب `THIS_ENTITY`
- يستخدم `BiMap<Identifier, ContextKeySet>` للسياقات المسجلة

### ArmorSlotFunction.java
- دالة غنائم تضع العنصر مباشرة في فتحة المعدات
- تمتد `LootItemConditionalFunction`
- CODEC يستخدم `RecordCodecBuilder` مع `EquipmentSlot.CODEC`

---

## 📋 الجزء 16: ModCompat.java

- **معطل بالكامل** (الكود مُعلق في تعليقات)
- كان يدعم مود Musket (بندقية)
- يشمل: UseMusketGoal, shootGun, reloadMusketAnim, holdMusketAnim

---

## ⚠️ الجزء 17: نقاط تحتاج انتباه / مشاكل محتملة

### 1. GuardSounds - تغيير Holder → مباشر
- النسخة الأصلية: `Holder<SoundEvent>` مع `.value()`
- النسخة الحالية: `SoundEvent` مباشر
- **يجب فحص**: كل استدعاء `GuardSounds.GUARD_YES` هل يمرر `SoundEvent` مباشرة أم لا يزال يحتاج `.value()`

### 2. LivingChangeTargetEvent - لا يوجد بديل مباشر في Fabric
- `onMobSetTarget()` موجود لكن لا يُستدعى من Mixin فعلي
- **يحتاج**: إنشاء Mixin على `Mob.setTarget()` لاستدعاء `HandlerEvents.onMobSetTarget()`

### 3. LivingDamageEvent - لا يوجد بديل كامل في Fabric
- منطق تقليل ضرر سهام الحراس للقرويين غير مطبّق بالكامل
- **يحتاج**: إنشاء Mixin على نظام الضرر أو استخدام `ServerLivingEntityEvents.ALLOW_DAMAGE`

### 4. GuardDataAttachments - لا يُحفظ مع العالم
- البيانات في ConcurrentHashMap تُفقد عند إيقاف الخادم
- **يحتاج**: نظام حفظ/تحميل للبيانات المرفقة

### 5. SpawnEggItem - ألوان غير مؤكدة
- الألوان `0x5A3F28, 0x3C89A6` للحراس و `0x7B5E3A, 0x7B8FA6` للوهمي قد لا تكون صحيحة
- **يجب فحص**: ملفات JSON لبيض التفقيط في النسخة الأصلية

### 6. processResources - توسيع إصدار فقط
- `fabric.mod.json` يوسّع `${version}` فقط
- لا يوسّع باقي الخصائص (قد يحتاج توسيع إضافي)

---

## 📊 إحصائيات الملفات

| الفئة | عدد الملفات | عدد الأسطر (تقريبي) |
|-------|-------------|---------------------|
| Java (الأساسية) | 10 | ~800 |
| Java (Guard) | 1 | ~1933 |
| Java (AI Goals) | 3 | ~100 |
| Java (AI Tasks) | 5 | ~400 |
| Java (Client) | 7 | ~700 |
| Java (Networking) | 6 | ~200 |
| Java (Mixins) | 3 | ~150 |
| Java (Config) | 1 | ~278 |
| Java (Loot) | 2 | ~80 |
| موارد JSON | ~25 | ~500 |
| نصوص PNG | ~20 | - |
| بناء (Gradle) | 4 | ~70 |
| **المجموع** | **~87** | **~5211+** |
