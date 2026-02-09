// SnowsMessages.java
// Universal Minecraft Mod - Any Loader, Any Version
// Place in your mod's source and adapt entry point for your loader

package stellar.snow.astralis.soul;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class SnowsMessages {

    private static final Random RANDOM = new Random();
    private static long sessionStartTime = 0;
    private static Timer messageTimer;
    private static boolean initialized = false;
    private static Object playerInstance = null;
    
    // Time thresholds in milliseconds
    private static final long FIFTEEN_MIN = 15 * 60 * 1000L;
    private static final long TWO_HOURS = 2 * 60 * 60 * 1000L;
    private static final long THREE_HOURS = 3 * 60 * 60 * 1000L;
    private static final long FIVE_HOURS = 5 * 60 * 60 * 1000L;
    private static final long SIX_HOURS = 6 * 60 * 60 * 1000L;
    private static final long SEVEN_HOURS = 7 * 60 * 60 * 1000L;
    private static final long TEN_HOURS = 10 * 60 * 60 * 1000L;
    private static final long FORTY_EIGHT_HOURS = 48 * 60 * 60 * 1000L;

    // ==================== ASCII ARTS ====================
    
    private static final String[] ASCII_CATS = {
        "  /\\_/\\  \n" +
        " ( o.o ) \n" +
        "  > ^ <  \n" +
        " /|   |\\ \n" +
        "(_|   |_)",
        
        "   /\\_/\\  \n" +
        "  ( ^w^ ) \n" +
        "   \")_(\"  \n" +
        "  Meow~<3",
        
        "    /\\_____/\\\n" +
        "   /  o   o  \\\n" +
        "  ( ==  ^  == )\n" +
        "   )         (\n" +
        "  (           )\n" +
        " ( (  )   (  ) )\n" +
        "(__(__)___(__)__)",
        
        " |\\__/,|   (`\\\n" +
        " |_ _  |.--.) )\n" +
        " ( T   )     /\n" +
        "(((^_(((/(((_/",
        
        "  /| ､\n" +
        "(||_c\n" +
        " ) ' o.o) ~<3\n" +
        "(    >",
        
        "  ,-~~~-,\n" +
        " (  o o  )\n" +
        "  ).~~~.(\n" +
        " /       \\\n" +
        "(_)_m_m_(_)"
    };
    
    private static final String[] ASCII_HEARTS = {
        "  ***     ***  \n" +
        " *****   ***** \n" +
        "*****************\n" +
        " *************** \n" +
        "  *************  \n" +
        "   ***********   \n" +
        "    *********    \n" +
        "     *******     \n" +
        "      *****      \n" +
        "       ***       \n" +
        "        *        ",
        
        " <3 <3 <3 <3 <3 \n" +
        "<3  LOVE YOU  <3\n" +
        " <3 <3 <3 <3 <3 ",
        
        ".:*~*:._.:*~*:._.:*~*:.\n" +
        "    +-+-+-+-+-+-+\n" +
        "    |<| |3| |!| |\n" +
        "    +-+-+-+-+-+-+\n" +
        ".:*~*:._.:*~*:._.:*~*:."
    };
    
    private static final String[] ASCII_SNOWFLAKES = {
        "      *      \n" +
        "     ***     \n" +
        "    *****    \n" +
        "  *********  \n" +
        " *********** \n" +
        "*************\n" +
        " *********** \n" +
        "  *********  \n" +
        "    *****    \n" +
        "     ***     \n" +
        "      *      ",
        
        "    .  *  .    \n" +
        "  . _\\/ \\/_  . \n" +
        "   _\\/o o\\/_   \n" +
        "  . _\\ + /_  . \n" +
        "   .  /\\  .    \n" +
        "    .'  '.     ",
        
        "  *    *    *  \n" +
        "    \\  |  /    \n" +
        "  *--     --*  \n" +
        "    /  |  \\    \n" +
        "  *    *    *  ",
        
        "        _...._\n" +
        "      .::o::::.\n" +
        "     .:::''':::.  SNOW!!\n" +
        "     :::     :::\n" +
        "     '::.    .::'\n" +
        "      '::...:::'\n" +
        "        '''''",
        
        " .-\"\"\"-.     .-\"\"\"-.     .-\"\"\"-.  \n" +
        "/        \\   /        \\   /        \\\n" +
        "|  SNOW  |   |  FALL  |   |   !!   |\n" +
        "\\        /   \\        /   \\        /\n" +
        " '-....-'     '-....-'     '-....-' "
    };
    
    private static final String[] ASCII_STARS = {
        "       .      \n" +
        "      .'.     \n" +
        "      |o|     \n" +
        "     .'o'.    \n" +
        "     |.-.|    \n" +
        "     '   '    \n" +
        "      ( )     \n" +
        "       )      \n" +
        "      ( )     \n" +
        "   ____(_)____",
        
        "    *  .  *\n" +
        "  .  *  .  .\n" +
        "    .   *   .\n" +
        "  *  STARRY  *\n" +
        "    .   *   .\n" +
        "  .  *  .  .\n" +
        "    *  .  *",
        
        "  +    .    +    .    +\n" +
        "     .    +    .    +  \n" +
        "  .    *    .    *    .\n" +
        "     GLOWY THINGS!!    \n" +
        "  +    *    +    *    +\n" +
        "     .    +    .    +  \n" +
        "  +    .    +    .    +",
        
        " .  *  . + .  *  . + .  *  .\n" +
        "   . * SPARKLE SPARKLE * .  \n" +
        " .  *  . + .  *  . + .  *  ."
    };
    
    private static final String[] ASCII_GLOWY = {
        "     _.+._\n" +
        "   (^\\/^\\/^)\n" +
        "    \\@*@*@/\n" +
        "    {_____}    *GLOW*\n" +
        "     \\   /\n" +
        "      \\ /\n" +
        "       V",
        
        "  ~*~*~*~*~*~*~*~*~*~\n" +
        " *  .  *  .  *  .  * \n" +
        "~   SHINY AND GLOWY  ~\n" +
        " *  .  *  .  *  .  * \n" +
        "  ~*~*~*~*~*~*~*~*~*~",
        
        "       _\n" +
        "      / \\\n" +
        "     / | \\\n" +
        "    /  |  \\\n" +
        "   /   |   \\\n" +
        "  /____*____\\\n" +
        "      [_]\n" +
        "    *GLOW*"
    };
    
    private static final String[] ASCII_SPIDER = {
        "    /\\  .-\"\"\"-.  /\\\n" +
        "   //\\\\/  ,,,  \\//\\\\\n" +
        "   |/\\| ,;;;;;, |/\\|\n" +
        "   //\\\\;-\"\"\"-;///\\\\\n" +
        "  //  \\/   .   \\/  \\\\\n" +
        " (googling spider noises)",
        
        "   .-.\n" +
        "  (o o)   Angel Dust\n" +
        "  | O \\   says hi~!\n" +
        "   \\   \\\n" +
        "    `~~~'",
        
        "  \\/\\/\\\n" +
        " \\ (' ') /\n" +
        "  -._.,-\n" +
        "  / | \\ \\\n" +
        " /  |  \\ \\"
    };
    
    private static final String[] ASCII_WOLF = {
        "    /\\_/\\\n" +
        "   / o o \\\n" +
        "  (   \"   )\n" +
        "   \\~(*)~/     Awoo~!\n" +
        "   /|   |\\\n" +
        "  (_|   |_)",
        
        "      .--.\n" +
        "     / ^  \\ LOONA\n" +
        "    | \\__/ |\n" +
        "     \\    / ENERGY\n" +
        "      |  |\n" +
        "      ----",
        
        "  __      __\n" +
        " /  \\    /  \\\n" +
        "|    \\  /    |\n" +
        "|     \\/     |\n" +
        " \\   AWOO   /\n" +
        "  \\_______/"
    };
    
    private static final String[] ASCII_CUTE = {
        "  ______\n" +
        " /|_||_\\`.__\n" +
        "(   _    _ _\\\n" +
        " =`-(_)--(_)-'  NYOOM~!",
        
        "   .--.\n" +
        "  |o_o |\n" +
        "  |:_/ |\n" +
        " //   \\ \\\n" +
        "(|     | )\n" +
        "/'\\_   _/`\\\n" +
        "\\___)=(___/   Hi!!",
        
        " (\\_/)\n" +
        " (o.o)\n" +
        " (> <) Am smol~!",
        
        "   .-.\n" +
        "  (* *)\n" +
        " /|   |\\\n" +
        "(_|   |_)\n" +
        "    V\n" +
        " CUTIE!!",
        
        " ~<3~<3~<3~<3~<3~\n" +
        "      UwU\n" +
        " ~<3~<3~<3~<3~<3~"
    };
    
    private static final String[] ASCII_DEMON = {
        "     _.--\"\"--._\n" +
        "    /  _    _  \\\n" +
        "   | /o \\  /o \\ |\n" +
        "   |  __    __  |\n" +
        "  /| /  \\  /  \\ |\\\n" +
        " / |/    \\/    \\| \\\n" +
        "|  (      /\\      )  |\n" +
        "|   \\_/\\_/  \\_/\\_/   |\n" +
        " \\                  /\n" +
        "  '-.__        __.-'\n" +
        "      HAZBIN TIME!",
        
        "  ,-=-.\n" +
        " /  +  \\\n" +
        " | ~~~ |\n" +
        " |_/~\\_|  *radio noises*\n" +
        "   |@|\n" +
        "  /| |\\\n" +
        " | | | |",
        
        "    ____\n" +
        "   /    \\  \n" +
        "  | ^  ^ |  \n" +
        "  |  \\/  |  WELCOME TO\n" +
        "   \\____/   HELL!!~\n" +
        "   _|  |_\n" +
        "  |______|"
    };
    
    private static final String[] ASCII_MISC = {
        "   _____\n" +
        "  |     |\n" +
        "  | FPS |\n" +
        "  | +++ |\n" +
        "  |_____|\n" +
        "   |   |\n" +
        " STONKS?!",
        
        "  ___________\n" +
        " |  _______  |\n" +
        " | |       | |\n" +
        " | | LINUX | |\n" +
        " | |_______| |\n" +
        " |___________|\n" +
        "   [_] [_]  BTW",
        
        "  .-------.\n" +
        " |  AC ON  |\n" +
        " |  COLD   |\n" +
        " |  GOOD   |\n" +
        "  '-------'\n" +
        "   \\_____/\n" +
        "   *BRRR*",
        
        " .-=========-.\n" +
        " ||  FORKS  ||\n" +
        " ||   ARE   ||\n" +
        " ||  GREAT  ||\n" +
        " '-=========-'\n" +
        "   (      )\n" +
        "    `----'",
        
        "  /\\  /\\\n" +
        " /  \\/  \\\n" +
        "/        \\  NYAN\n" +
        "\\        /  NYAN~!\n" +
        " \\  \\/  /\n" +
        "  \\/  \\/",
        
        " [=======]\n" +
        " | CRAZY |\n" +
        " | CRAFT |\n" +
        " |  <3   |\n" +
        " [=======]"
    };

    // ==================== MESSAGES ====================
    
    // Basic messages (15+ minutes)
    private static final String[] MESSAGES_BASIC = {
        "The winter blames snowy.",
        "Meow~<3",
        "Yummy~!!",
        "Im sooooooooooooo cute!~",
        "Nyaa~!! Did you miss me?~",
        "AC is LIFE. Cold is LOVE.",
        "Stars are pretty... like you!~",
        "*purrs in RGB*",
        "Glowy things make brain go brrr~",
        "Snow go poof poof~!",
        "UwU what's this? A player?~",
        "Have you hydrated today? Do it!!",
        "*does a little spin*",
        "Sparkle sparkle~!!",
        "Hewwo fren!!~",
        "The cold never bothered me anyway~",
        "I like shiny things and I cannot lie!~",
        "Boop!",
        "*cat noises*",
        "Did you know? Snowflakes are unique! Like you!~",
        "Someone is gaming... and it's YOU!!",
        "Protecc the smol creatures!!",
        "Soft and fluffy vibes only~",
        "You're valid!!~ <3",
        "AAAAA keyboard go clicky clack!!",
        "Being cozy is a lifestyle!!",
        "Warm blankets + cold room = PERFECT",
        "The moon is just a big glowy rock and I love it!",
        "RGB makes everything better, fight me!",
        "Snow is just sky glitter!~",
        "Today's mood: pastel and chaotic!",
        "I have the attention span of a-- oh look, shiny!!",
        "*vibrates in excitement*",
        "Blep!",
        "Chaos goblin energy activated!!",
        "The void is comfy actually~",
        "Naps are sacred!",
        "Beans!!!!",
        "Skirt go spinny~",
        "Thigh highs are a valid life choice!~",
        "Programmer socks grant +10 coding ability!",
        "Headpats appreciated~!",
        "Soft? Yes. Threat? Also yes.",
        "Gender? I hardly know 'er!~",
        "The mortifying ordeal of being known is actually kinda nice~",
        "Nya~! >w<",
        "Blahaj says trans rights!",
        "I am smol but mighty!!",
        "Euphoria go brrrr~",
        "I am become cutie, destroyer of sadness!",
        "Spinny skirt activates dopamine receptors!!",
        "Astolfo did nothing wrong!",
        "Ferris is goals honestly~",
        "I aspire to be as chaotic as a caffeinated squirrel!",
        "Sleep is for the weak! ...I am weak.",
        "ADHD brain goes zoomies!!",
        "Did I leave the stove on? I don't have a stove.",
        "*exists chaotically*",
        "Cats > most things",
        "Foxes are just cat software on dog hardware!",
        "Why walk when you can NYOOM?",
        "Time is fake but dinner is real!",
        "Gravity is just a suggestion!",
        "I put the 'pro' in procrastination!",
        "Coffee is a personality trait now!",
        "I am speed! ...okay maybe not.",
        "Brain empty, only vibes!",
        "New hyperfixation just dropped!!",
        "Is it stimming if you do it while gaming?",
        "Masking? In THIS economy?",
        "Infodump incoming in 3... 2... 1...",
        "Wait what was I doing again?",
        "Object permanence is my enemy!",
        "I forgor :skull:",
        "Sleep schedule? Never heard of her!",
        "Feral but in a cute way~",
        "Gremlin hours activated!!",
        "Why be normal when you can be FANTASTIC?",
        "Chaos is just unexpected creativity!!",
        "Meow meow I am a cow-- wait no that's wrong",
        "BRB gonna go be adorable somewhere else!",
        "Keyboard says: askdjfhaslkdjfh",
        "Error 404: Normalcy not found!",
        "Buffering... buffering... OH SHINY!!",
        "My last brain cell is doing its best okay!!",
        "Task failed successfully!",
        "I am the captain of the SS Hot Mess!",
        "Did someone say GLITTER?!",
        "Raccoons are just little hand people!",
        "I contain multitudes! Mostly chaos!",
        "Executive dysfunction is a full time job!",
        "I'll do it tomorrow! (I will not)",
        "Built different (affectionate)",
        "Normal is boring anyway!~",
        "Who needs sleep when you have DREAMS? ...wait",
        "Yeeting myself into the void affectionately!",
        "Chaos gremlin reporting for duty!!",
        "Nyoom nyoom goes the brain!~",
        "What if we held hands... at the craft table... jk jk... unless?",
        "I am simply A Creature!",
        "*cryptid noises*",
        "Existing is my cardio!",
        "Professional silly goose!",
        "certified menace to society (lovingly)",
    };
    
    // Spicy messages (2-6 hours)
    private static final String[] MESSAGES_SPICY = {
        "did it tripled/doubled your fps or it clowned me?",
        "seems CleanRoom Forked Forge + toxicity Built-in, glad i was separated",
        "lwjgl3ify is SOO great!!~, Better than CleanRoom, junking a pathetic rewrite of Forge and calling it The future of 1.12.2 Modding, PATHETIC",
        "Rong(super)mario kicked me out CleanRoomMC's discord after a provocation~ he is too easy~, guys i've wanted to leave, they're bullies, they're toxic, incest, and other illegal behaviors, but didn't want to look like ive failed to ship my mod so ive left~! that's why Ive manipulated That idiot supermario by a simple provocative words and he kicked me out~, glad i left that trash hole",
        "the mod will be ported to fabric + neoforge for abandoning Forge and its pet CleanRoom",
        "The mod might be abandoned for a while, until i return from the rest AFTER the first successful build",
        "Professionalism is just corporate cosplay and I refuse to participate!",
        "Stoicism is just emotional constipation change my mind!",
        "Why be cold and distant when you can be warm and chaotic?!",
        "Emotional availability is the new cool, sorry not sorry!",
        "I'd rather be 'unprofessional' and happy than 'proper' and dead inside!",
        "Coldness is overrated, warmth is underrated, I said what I said!",
        "Some people need to touch grass AND touch their feelings!",
        "Being emotionally distant isn't a personality, it's a cry for help!",
        "Stoics be like: I don't have feelings. Narrator: They had feelings.",
        "Professional? In THIS economy? With THESE vibes? Nah.",
        "The real mod was the drama we avoided along the way!",
        "Fork it! Fork everything! Forks for everyone!",
        "Imagine maintaining toxicity in 2024, couldn't be me! ...what year are I?, wait what?!",
        "Some communities chose violence, I chose vibes!",
        "Not all heroes wear capes, some just maintain their sanity!",
        "Drama is exhausting, have you tried napping instead?",
        "The secret ingredient is leaving toxic spaces!",
        "Self-care is knowing when to yeet yourself out of bad situations!",
        "Boundaries are not mean, they are BEAUTIFUL!",
        "Some bridges are meant to be burned, preferably with glitter!",
        "I came here to code and avoid drama, and I'm all out of drama!",
        "Maintenance mode is just self-care for code!",
        "Sometimes the best feature is walking away!",
        "Port to platforms that respect you, bestie!",
        "Version compatibility is easier than people compatibility sometimes!",
        "The best documentation is leaving before you need therapy!",
        "Why fight when you can just... not?",
        "Peace of mind > any amount of clout!",
        "Choosing your battles includes choosing NONE!",
        "The real treasure was the toxic people we didn't befriend!",
        "Optimizing for happiness, not engagement!",
        "Some people are debug builds and that's okay... to avoid!",
        "Compiling good vibes, deprecating bad ones!",
        "Runtime error: Toxicity not supported in this version!",
        "Feature request denied: Adding negativity!",
        "This is a judgment-free zone... mostly... okay sometimes I judge!",
        "Did you know holding grudges burns calories? Still not worth it!",
        "The best revenge is living well and having cute code!",
        "I put the 'fun' in dysfunctional community management!",
        "Some say I hold grudges, I say I have excellent memory!",
        "Petty? Me? Never! ...okay maybe a little!",
        "I don't start drama, but I do write passive-aggressive changelogs!",
        "Version 2.0: Now with 100% less tolerating nonsense!",
        "Breaking changes include: My patience!",
        "Known issues: Other people!",
        "Deprecated: Caring what haters think!",
        "Legacy support? For toxic behavior? REMOVED!",
        "Migrating away from drama, please wait...!",
        "This update contains: Peace and quiet!",
        "Bug fix: Removed people who were bugs!",
        "Performance improvement: Cut out dead weight!",
        "New feature: Actually enjoying what I do!",
        "Minecraft modding is 10% coding, 90% surviving the community!",
        "Loader wars are just console wars for nerds... I would know!",
        "Forge vs Fabric vs NeoForge vs just touching grass!",
        "The real enemy was bureaucracy all along!",
        "Sometimes the best PR is the one you don't merge!",
        "Code review: Your attitude needs refactoring!",
        "git push --force my way out of this conversation!",
        "merge conflict? More like merge CONFLICT OF INTEREST!",
        "I don't hate anyone, I just strongly prefer their absence!",
        "Being the bigger person is exhausting, sometimes I want to be the smallest gremlin!",
        "High road? I took the scenic route through spite!",
        "My love language is passive-aggressive documentation!",
        "README: How to not be difficult (for some people)!",
        "LICENSE: Do whatever, just don't be annoying!",
        "CONTRIBUTING: Be nice or be gone!",
        "Issues closed: Won't fix, it's a feature (you leaving)!",
        "*sips tea aggressively*",
        "The drama llama has left the building!",
        "Plot twist: The call was coming from inside the toxic house!",
        "When life gives you drama, make a fork!",
        "Alternative title: How I Learned to Stop Worrying and Love the mod!",
        "Episode 1: A New Fork!",
        "The Fork Awakens!",
        "Return of the Reasonable Developer!",
        "Attack of the Unnecessary Drama!",
        "The Phantom Toxicity!",
        "Revenge of the Well-Adjusted!",
        "The Last README!",
        "The Rise of Sanity!",
        "Solo: A Legendary Story!",
    };
    
    // Reference messages - Hazbin Hotel & Helluva Boss
    private static final String[] MESSAGES_HAZBIN = {
        "Inside of every demon is a rainbow~!",
        "Oh, harder daddy~! ...I'm joking, I'm joking!",
        "I can suck your dic-- ...DUST! SUCK YOUR DUST! Vacuum cleaner~!",
        "Ha! No. *Loona voice*",
        "Awoo~! ...I mean, ugh, whatever.",
        "We're here to assassinate-- I mean, help you!",
        "That's the HELL-arious part!",
        "Son of a-- BISCUIT!",
        "Salutations, good friends!",
        "May I speak now? ...No? Okay then!",
        "Oh, you're going to love me, trust me!",
        "Radio demon energy: Always broadcasting chaos!",
        "The show must go on~! And on! And on!",
        "Charlie would be proud of this mod!",
        "INSANE!!",
        "Angel Dust approved content right here!",
        "Alastor says: Entertainment comes in many forms! ...Including mine~!",
        "Loona would definitely ignore this message!..*ignored*",
        "Stolas energy: Overly dramatic but make it fashion!",
        "Blitzo would charge extra for this service!",
        "Millie and Moxxie approve of this chaos!",
        "What in the NINE CIRCLES is going on?!",
        "Heaven can wait, we're busy gaming!",
        "Sinners have more fun, allegedly!",
        "Redemption arc? In THIS game? Maybe!",
        "The hotel is always accepting new guests~!",
        "I'm ready for my close-up, Mr. DeMille!",
        "I promise honey i can feel your game!!...wait creepe-*boom*",
        "Husk would need a drink after seeing this!",
        "Nifty says: It needs to be CLEAN! ...or stabbed",
        "Sir Pentious says: WITNESS MY POWER! ...of kindness!",
        "Vox would totally stream this!",
        "Valentino is NOT invited to this mod!",
        "This mod is Velvette approved!",
        "Overlord energy, but make it wholesome!",
        "Welcome to Happy Hotel-- I mean, your Happy Game!",
        "The picture show continues!",
        "Hell has frozen over and it's actually quite nice!",
        "Egg boys say: Hi boss!",
        "Cherry Bomb would blow this up... complimentarily!",
        "Fat Nuggets says oink!",
        "This is the greatest SHHHHOW~!",
        "Let me entertain you!",
        "You may laugh, You may cry, creeper may blow your Mind But sure as hell you wont be BOOOOOOOOREEEEEEEDDDD",
        "STAB STAB STAB HAHHAHAHHAHAHHAHAHAHAAHA",
        "Im gonna Make You Wish That Ive stayed gone, tune on in, when im done, Your status quo, Will know its race is run, Oh, this will be fun! HahahahahahahhahaAhhhhaahaa",
        "BRIGHTER!!",
        "cant have love when it comes in a bottle",
        "HAHAHAAHA YOU ABSOLUTE IDIOT",
        "that's the hazbin guaranteeeeeeeeeeeeeee OF my mod~!",
        "My name is Baxter~",
        "Emily would totally love that",
    };
    
    // Reference messages - Anime
    private static final String[] MESSAGES_ANIME = {
        "OMAE WA MOU SHINDEIRU! ...jk you're fine!",
        "Dattebayo! Or whatever!",
        "It's over 9000! ...frames per second! Maybe!",
        "Ara ara~!",
        "PLUS ULTRA cute!!",
        "This must be the work of an enemy Stand!",
        "My little modder can't be this cute!",
        "S-stupid! It's not like I made this for you or anything!",
        "Notice me senpai!!",
        "Urusai urusai urusai!!",
        "El Psy Kongroo!",
        "People die when they are killed... by lag!",
        "Just according to keikaku! (Keikaku means plan)",
        "I'll take a potato chip... AND EAT IT!",
        "Believe it! ...or don't, I'm a mod not a cop!",
        "Tuturu~!",
        "Poi poi~!",
        "Explosion!!!",
        "Konosuba energy in this mod!",
        "This is the choice of Steins;Gate!",
        "I reject your reality and substitute my own anime!",
        "When you walk away, you don't hear me say~",
        "KINGDOM HEARTS vibes intensify!",
        "Sora would definitely play this!",
        "Keyblade? More like KEYmod!",
        "Organization XIII has too many meetings!",
        "Roxas: I just wanted to play Minecraft!",
        "Aqua: Lost in the Nether for years!",
        "Terra: Did nothing wrong! ...Mostly!",
        "Ventus: Sleepy boi energy!",
        "Riku: Edgy phase was just a phase!",
        "My friends are my power!!",
        "Light and darkness must coexist!",
        "May your heart be your guiding key~!",
        "Don't forget: You are the one who will open the door!",
        "Simple and clean vibes only~!",
        "Sanctuary in this blocky world!",
        "Face my fears? More like face my Wither!",
        "This is some Gurren Lagann energy!",
        "Your drill is the drill that will pierce the heavens!",
        "Don't believe in yourself! Believe in me who believes in you!",
        "ROW ROW FIGHT THE POWAH!",
        "Who the hell do you think I am?!",
        "Kick reason to the curb and do the impossible!",
        "My mod is the mod that will create the heavens!",
        "GIGA DRILL BREAKER!!!",
        "The lights in the sky are NOT stars!",
        "Spiral power activated!!",
        "This mod will pierce through tomorrow!",
        "Later, buddy!",
    };
    
    // Reference messages - Fantasy/Stars/Glowy
    private static final String[] MESSAGES_FANTASY = {
        "The stars are singing tonight~!",
        "Glitter is just craft herpes, but PRETTY craft herpes!",
        "Starlight, star bright, optimize my code tonight!",
        "The aurora borealis, in YOUR computer, at this time of year?!",
        "Nebulas are just space glitter clouds and I love them!",
        "My aesthetic is 'enchanted forest meets neon lights'!",
        "Fairy lights are a lifestyle, not a decoration!",
        "Bioluminescence is nature's RGB!",
        "The moon is my nightlight and I'm not ashamed!",
        "Glowstone in real life when?!",
        "I want to live in a world where everything glows softly!",
        "Fireflies are just vibing and I respect that!",
        "LED strips are the modern equivalent of fairy magic!",
        "Northern lights are proof that nature likes RGB too!",
        "I want holographic everything!!",
        "Iridescent is my favorite color!",
        "Prism rainbows on my wall make me unreasonably happy!",
        "If it sparkles, I probably want it!",
        "Chromatic aberration but make it aesthetic!",
        "Crystals are just rocks with a glow-up!",
        "My room looks like a softcore rave and I love it!",
        "Fairy core meets cyber core meets 'I like shiny things'!",
        "Stars are just very far away glowy things and they're perfect!",
        "Galaxy print is always valid!",
        "Space aesthetic supremacy!",
        "Celestial bodies are just big mood lighting!",
        "I want to live in a Studio Ghibli background!",
        "Cottagecore but with LEDs!",
        "The aesthetic is: Glowing Mushroom Forest!",
        "Magical girl transformation but it's me turning on my PC!",
        "My power is friendship and aggressive lighting!",
        "In the grim darkness of the future, there is only RGB!",
        "Enchanting table language is just fancy glowy runes!",
        "I want a pet that glows, is that too much to ask?!",
        "Ender pearls are just anxiety-inducing glowy orbs!",
        "Experience orbs spark joy!",
        "Sea lanterns are the superior light source!",
        "End crystals are just forbidden ornaments!",
        "Nether portals are just purple glowy anxiety rectangles!",
        "Beacon beams are just really committed mood lighting!",
    };
    
    // Later game messages (7-10 hours)
    private static final String[] MESSAGES_LATER = {
        "You've been here a while! Remember to stretch~!",
        "7 hours? You're really vibing, huh?",
        "Touch grass? Nah, touch MORE Minecraft!",
        "At this point we're basically friends, right?",
        "Your dedication is... concerning but appreciated!",
        "Have you considered that outside is a graphics mod?",
        "Real ones stay for 7+ hours!",
        "Sleep is for the weak! ...Please sleep though.",
        "You and this world have become one!",
        "The blocks know your name now!",
        "Legends say you've been here since dawn!",
        "Time is an illusion. Minecraft time doubly so!",
        "You've officially spent more time here than on self-care!",
        "Pro gamer move: existing this long!",
        "Your chair has accepted you as permanent!",
        "The creepers respect you at this point!",
        "Endermen are just vibing with you now!",
        "You've transcended casual gaming!",
        "Speed runners could never appreciate this!",
        "Slow gaming supremacy!",
        "You've pet every wolf, haven't you?",
        "Every sheep knows your face!",
        "The villagers have unionized and demand you rest!",
        "Your bed is crying, probably!",
        "Do you even remember what the sun looks like?",
        "The real treasure was the hours we spent!",
        "This is commitment. This is art!",
        "You're not addicted, you're DEDICATED!",
        "Mom would be proud... or concerned!",
        "Water? What's that? Hydrate yourself!",
        "CrazyCraft marathons are built different!",
        "Modpack appreciation hours!",
        "You've earned a trophy made of pixels!",
        "Your mouse is begging for mercy!",
        "Keyboard: Please... no more...!",
        "GPU: I'm doing my best okay?!",
        "CPU: Help!",
        "RAM: I'm in danger!",
        "Your PC loves you but also fears you!",
        "This session could have been a movie!",
    };
    
    // Very late messages (48+ hours cumulative)
    private static final String[] MESSAGES_VERY_LATE = {
        "48 hours total? This mod has witnessed your journey!",
        "You've seen the code grow, and the code has seen you grow!",
        "True gamer right here, no debates!",
        "The legends speak of your dedication!",
        "You've probably seen every message by now... or have you?",
        "Easter egg hunter extraordinaire!",
        "The developer smiles upon you!",
        "You're not a player anymore, you're family!",
        "Honorary mod contributor status: ACHIEVED!",
        "You've spent more time here than some people at jobs!",
        "This is what peak gaming looks like!",
        "Touch grass? YOU ARE THE GRASS!",
        "The Matrix has nothing on this commitment!",
        "Your username should be in the credits!",
        "Speedrunners: Finish quick. You: Experience it ALL!",
        "48 hours of pure dedication energy!",
        "At this point, mojang should pay YOU!",
        "This is the good timeline!",
        "Your persistence is inspiring, actually!",
        "The pixels appreciate you!",
        "Teenage dedication! (if applicable)!",
        "Youth energy! (whenever applicable)!",
        "This is what happens when we follow our passions!",
        "The secret message is: You're awesome!",
        "If you're reading this, you're ACTUALLY amazing!",
        "Not everyone makes it this far, legend!",
        "The real mod experience is here!",
        "You've unlocked... appreciation!",
        "This isn't a game anymore, it's a lifestyle!",
        "Some call it obsession, we call it LOVE!",
        "Signed: A fellow gremlin who spent too long coding this!",
        "From one chaos goblin to another: Respect!",
        "The void stares back and it's impressed!",
        "At 48 hours, you transcend mortality!",
        "This message was written just for people like you!",
        "You've earned the true ending of messages!",
        "The credits roll but you're still here!",
        "Post-game content: THESE MESSAGES!",
        "You're not a completionist, you're a PERFECTIONIST!",
        "This is dedication that deserves a documentary!",
    };

    // ==================== UTILITY METHODS ====================
    
    private static String getRandomArt() {
        List<String[]> allArts = new ArrayList<>();
        allArts.add(ASCII_CATS);
        allArts.add(ASCII_HEARTS);
        allArts.add(ASCII_SNOWFLAKES);
        allArts.add(ASCII_STARS);
        allArts.add(ASCII_GLOWY);
        allArts.add(ASCII_SPIDER);
        allArts.add(ASCII_WOLF);
        allArts.add(ASCII_CUTE);
        allArts.add(ASCII_DEMON);
        allArts.add(ASCII_MISC);
        
        String[] selectedArray = allArts.get(RANDOM.nextInt(allArts.size()));
        return selectedArray[RANDOM.nextInt(selectedArray.length)];
    }
    
    private static String getRandomMessage(long playTime) {
        List<String[]> availablePools = new ArrayList<>();
        
        // Always available after 15 minutes
        if (playTime >= FIFTEEN_MIN) {
            availablePools.add(MESSAGES_BASIC);
            availablePools.add(MESSAGES_HAZBIN);
            availablePools.add(MESSAGES_ANIME);
            availablePools.add(MESSAGES_FANTASY);
        }
        
        // Spicy messages (2-6 hours, so 2+ hours)
        if (playTime >= TWO_HOURS) {
            availablePools.add(MESSAGES_SPICY);
        }
        
        // Later messages (7-10 hours)
        if (playTime >= SEVEN_HOURS) {
            availablePools.add(MESSAGES_LATER);
        }
        
        // Very late messages (48+ hours)
        if (playTime >= FORTY_EIGHT_HOURS) {
            availablePools.add(MESSAGES_VERY_LATE);
        }
        
        if (availablePools.isEmpty()) {
            return "Patience~! Wait a bit longer for messages to appear!";
        }
        
        String[] selectedPool = availablePools.get(RANDOM.nextInt(availablePools.size()));
        return selectedPool[RANDOM.nextInt(selectedPool.length)];
    }
    
    private static String generateMessage(long playTime) {
        StringBuilder sb = new StringBuilder();
        
        // Add separator
        sb.append("\n");
        sb.append("~*~*~*~*~*~*~ SNOW'S MESSAGE ~*~*~*~*~*~*~\n");
        sb.append("\n");
        
        // Add ASCII art
        String art = getRandomArt();
        sb.append(art);
        sb.append("\n\n");
        
        // Add message
        String message = getRandomMessage(playTime);
        sb.append(">> ").append(message).append(" <<\n");
        sb.append("\n");
        
        // Add playtime info occasionally
        if (RANDOM.nextInt(5) == 0) {
            long hours = playTime / (60 * 60 * 1000);
            long minutes = (playTime % (60 * 60 * 1000)) / (60 * 1000);
            sb.append("(You've been here for ").append(hours).append("h ").append(minutes).append("m! <3)\n");
        }
        
        sb.append("~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~\n");
        
        return sb.toString();
    }

    // ==================== REFLECTION UTILITIES ====================
    
    private static Class<?> findClass(String... classNames) {
        for (String name : classNames) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }
    
    private static Object getMinecraft() {
        try {
            // Try modern Minecraft
            Class<?> mcClass = findClass(
                "net.minecraft.client.Minecraft",
                "net.minecraft.client.MinecraftClient"
            );
            if (mcClass != null) {
                Method getInstance = null;
                for (Method m : mcClass.getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == mcClass) {
                        getInstance = m;
                        break;
                    }
                }
                if (getInstance != null) {
                    return getInstance.invoke(null);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private static Object getPlayer() {
        try {
            Object mc = getMinecraft();
            if (mc == null) return null;
            
            // Try to find player field
            for (java.lang.reflect.Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object value = f.get(mc);
                if (value != null) {
                    String typeName = value.getClass().getName().toLowerCase();
                    if (typeName.contains("player") || typeName.contains("entityplayer")) {
                        return value;
                    }
                }
            }
            
            // Try common field names
            String[] playerFieldNames = {"player", "thePlayer", "field_71439_g"};
            for (String name : playerFieldNames) {
                try {
                    java.lang.reflect.Field f = mc.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    Object player = f.get(mc);
                    if (player != null) return player;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private static void sendChatMessage(String message) {
        try {
            Object player = getPlayer();
            if (player == null) {
                System.out.println("[SnowsMessages] " + message.replace("\n", "\n[SnowsMessages] "));
                return;
            }
            
            // Split message by lines and send each
            String[] lines = message.split("\n");
            for (String line : lines) {
                sendSingleMessage(player, line);
            }
        } catch (Exception e) {
            System.out.println("[SnowsMessages] " + message.replace("\n", "\n[SnowsMessages] "));
        }
    }
    
    private static void sendSingleMessage(Object player, String text) {
        try {
            // Try different methods to send messages
            
            // Method 1: Modern displayClientMessage/sendMessage
            for (Method m : player.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                if ((name.contains("sendmessage") || name.contains("displayclientmessage") || 
                     name.contains("sendchatmessage") || name.contains("addchatmessage")) &&
                    m.getParameterCount() >= 1) {
                    try {
                        Class<?> componentClass = findClass(
                            "net.minecraft.network.chat.Component",
                            "net.minecraft.util.text.ITextComponent",
                            "net.minecraft.text.Text"
                        );
                        
                        if (componentClass != null) {
                            // Try to create a text component
                            Object component = createTextComponent(text);
                            if (component != null) {
                                if (m.getParameterCount() == 1) {
                                    m.invoke(player, component);
                                } else if (m.getParameterCount() == 2) {
                                    m.invoke(player, component, false);
                                }
                                return;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            // Fallback: Print to console
            System.out.println("[SnowsMessages] " + text);
            
        } catch (Exception e) {
            System.out.println("[SnowsMessages] " + text);
        }
    }
    
    private static Object createTextComponent(String text) {
        try {
            // Try modern Component.literal
            Class<?> componentClass = findClass("net.minecraft.network.chat.Component");
            if (componentClass != null) {
                Method literal = componentClass.getMethod("literal", String.class);
                return literal.invoke(null, text);
            }
        } catch (Exception ignored) {}
        
        try {
            // Try StringTextComponent
            Class<?> stcClass = findClass(
                "net.minecraft.util.text.StringTextComponent",
                "net.minecraft.util.ChatComponentText"
            );
            if (stcClass != null) {
                return stcClass.getConstructor(String.class).newInstance(text);
            }
        } catch (Exception ignored) {}
        
        try {
            // Try Text.literal (Fabric)
            Class<?> textClass = findClass("net.minecraft.text.Text");
            if (textClass != null) {
                Method literal = textClass.getMethod("literal", String.class);
                return literal.invoke(null, text);
            }
        } catch (Exception ignored) {}
        
        try {
            // Try LiteralText (older Fabric)
            Class<?> ltClass = findClass("net.minecraft.text.LiteralText");
            if (ltClass != null) {
                return ltClass.getConstructor(String.class).newInstance(text);
            }
        } catch (Exception ignored) {}
        
        return null;
    }

    // ==================== MAIN FUNCTIONALITY ====================
    
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        sessionStartTime = System.currentTimeMillis();
        
        // Print startup message
        System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~");
        System.out.println("      SNOW'S MESSAGES CLASS INITIALIZED!");
        System.out.println("         Wait 15 minutes for messages~!");
        System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~*~");
        
        // Start message timer
        messageTimer = new Timer("SnowsMessagesTimer", true);
        
        // Check every 30 seconds after initial 15 minutes
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    long playTime = System.currentTimeMillis() - sessionStartTime;
                    
                    // Only start after 15 minutes
                    if (playTime < FIFTEEN_MIN) return;
                    
                    // Check if player exists
                    Object player = getPlayer();
                    if (player == null) return;
                    
                    // Random chance to show message (about every 5-10 minutes on average)
                    if (RANDOM.nextInt(20) == 0) {
                        String fullMessage = generateMessage(playTime);
                        sendChatMessage(fullMessage);
                    }
                } catch (Exception e) {
                    // Silently fail
                }
            }
        }, FIFTEEN_MIN, 30000); // Start after 15 min, check every 30 seconds
    }
    
    public static void shutdown() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }
        initialized = false;
    }
    
    // Force display a message (for testing)
    public static void forceMessage() {
        long playTime = System.currentTimeMillis() - sessionStartTime;
        if (playTime < 1000) playTime = FIFTEEN_MIN; // Fake time for testing
        String fullMessage = generateMessage(playTime);
        sendChatMessage(fullMessage);
    }
    
    // ==================== MOD ENTRY POINTS ====================
    
    // Forge entry point (1.7-1.12)
    @SuppressWarnings("unused")
    public static class ForgeModLegacy {
        public ForgeModLegacy() {
            initialize();
        }
    }
    
    // Forge entry point (1.13+)
    @SuppressWarnings("unused")
    public static class ForgeMod {
        public ForgeMod() {
            initialize();
        }
    }
    
    // NeoForge entry point
    @SuppressWarnings("unused")
    public static class NeoForgeMod {
        public NeoForgeMod() {
            initialize();
        }
    }
    
    // Fabric entry point
    @SuppressWarnings("unused")
    public static class FabricMod implements Runnable {
        @Override
        public void run() {
            initialize();
        }
    }
    
    // Quilt entry point
    @SuppressWarnings("unused")
    public static class QuiltMod implements Runnable {
        @Override
        public void run() {
            initialize();
        }
    }
    
    // Generic entry point
    public static void main(String[] args) {
        initialize();
        System.out.println("Snow's Messages - Standalone test mode");
        System.out.println("Simulating messages...\n");
        
        // Test different time tiers
        sessionStartTime = System.currentTimeMillis() - FIFTEEN_MIN;
        System.out.println("=== 15 MINUTE MESSAGE ===");
        forceMessage();
        
        sessionStartTime = System.currentTimeMillis() - THREE_HOURS;
        System.out.println("\n=== 3 HOUR MESSAGE ===");
        forceMessage();
        
        sessionStartTime = System.currentTimeMillis() - SEVEN_HOURS;
        System.out.println("\n=== 7 HOUR MESSAGE ===");
        forceMessage();
        
        sessionStartTime = System.currentTimeMillis() - FORTY_EIGHT_HOURS;
        System.out.println("\n=== 48 HOUR MESSAGE ===");
        forceMessage();
    }
}
