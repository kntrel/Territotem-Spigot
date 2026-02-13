package com.kntrel.mc.territotem.io;

import com.jkantrell.yamlizer.yaml.YamlElement;
import com.jkantrell.yamlizer.yaml.YamlElementType;
import com.jkantrell.yamlizer.yaml.YamlMap;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class LangProvider {

    //ASSETS
    private record Language(String code, YamlMap map) {
        String getEntry(String path) {
            return Optional.ofNullable(this.map().gerFromPath(path))
                    .map(e -> e.get(YamlElementType.STRING))
                    .orElse(null);
        }
        void setEntry(String path, String value) {
            this.map.putInPath(path,new YamlElement(value));
        }
    }

    //FIELDS
    private String defaultLang_ = null;
    private Level loggingLevel_ = Level.FINEST;
    private final String langsPath_;
    private final JavaPlugin plugin_;
    private final HashMap<String, Language> langs_ = new HashMap<>();

    //CONSTRUCTORS
    public LangProvider(JavaPlugin plugin, String langsPath) {
        this.langsPath_ = langsPath;
        this.plugin_ = plugin;

        File langFolder = new File(this.plugin_.getDataFolder().getPath() + "/" + langsPath);
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        } else if (!langFolder.isDirectory()) {
            throw new NoSuchFieldError("The 'langPath' parameter must be a directory.");
        }
    }
    public LangProvider(JavaPlugin plugin, String langsPath, String defaultLang) {
        this(plugin,langsPath);
        this.setDefaultLanguage(defaultLang);
    }

    //GETTERS
    public String getLangsPath() {
        return this.plugin_.getDataFolder() + File.separator + this.langsPath_;
    }

    //SETTERS
    public void setDefaultLanguage(String key) {
        this.defaultLang_ = key;
        try {
            Language lang = this.loadLanguage_(key);
            if (lang == null) {
                this.log_(
                        "The lang file '" + key + ".yml' does not exist neither in " + this.plugin_.getName() + "'s internal resources, nor in the plugin's langs path.\n"
                                + "Set an existing default file or create it. Then restart the server.",
                        Level.SEVERE
                );
                this.defaultLang_ = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.defaultLang_ = null;
        }
    }
    public void setLoggingLevel(Level level) {
        this.loggingLevel_ = level;
    }

    //METHODS
    public String getEntry(Player player, String path, Object... params) {
        return this.getEntry(player.getLocale(),path, params);
    }
    public String getEntry(String locale, String path, Object... params) {
        return String.format(this.getNonFormattedEntry(locale,path), params);
    }
    public String getLangFileName(Player player) {
        Language lang = null;
        try {
            lang = this.pickLanguage_(player.getLocale());
        } catch (IOException e) { e.printStackTrace(); }
        if (lang == null) { return ""; }
        return lang.code() + ".yml";
    }
    public String getNonFormattedEntry(Player player, String path) {
        return this.getNonFormattedEntry(player.getLocale(), path);
    }
    public String getNonFormattedEntry(String locale, String path) {
        //Getting a lang file.
        Language lang = null;
        try {
            lang = this.pickLanguage_(locale);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (lang == null) {
            this.log_(
                "Unable to load a file for language code '" + locale+ "'. Please set a default language and make sure its file exists. Returning empty string.",
                    Level.SEVERE
            );
            return "";
        }

        //Retrieving entry
        String entry = lang.getEntry(path);

        //Checking and reacting if no entry was found
        if (entry == null) {
            StringBuilder log = new StringBuilder();
            log.append("Not an entry for path '").append(path).append("' in '").append(lang.code()).append(".yml' language file.");

            //Trying to load from default file
            if (this.defaultLang_ != null) {
                entry = this.langs_.get(this.defaultLang_).getEntry(path);
                if (entry != null) {
                    log.append(" Using default file instead.");
                    this.log_(log.toString(), Level.WARNING);
                } else {
                    log.append(" Not in default language file either.");
                }
            } else {
                log.append(" Not a default language loaded.");
            }

            //Setting entry to an empty string if not entry found
            if (entry == null) {
                log.append(" Returning empty string.");
                this.log_(log.toString(), Level.SEVERE);
                entry = "";
            }
        }
        //Returning entry if it's empty
        if (entry.length()<1) { return entry; }

        //Checking if and reaction if entry points to another file
        if (entry.startsWith("%/")) {
            String external = entry.substring(2);
            InputStream stream = this.loadResource_(external);
            StringBuilder log = new StringBuilder();
            log
                    .append("Language entry '")
                    .append(path)
                    .append("' in file '")
                    .append(lang.code())
                    .append(".yml' points to external file: '")
                    .append(external)
                    .append("'");

            //Reading the file
            entry = null;
            try {
                entry = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {               //Error reading
                log.append(", but there was an IOException when trying to read it.");
            } catch (NullPointerException e) {      //The file doesn't exist
                log
                        .append(", but it wasn't found in '")
                        .append(this.getLangsPath())
                        .append("' not in ").append(this.plugin_.getName())
                        .append("'s internal resources.");
            }

            //Reacting entry couldn't be read
            if (entry == null) {
                log.append(" Returning empty string.");
                return "";
            }

            //Assigning this entry to the Language's map.
            lang.setEntry(path, entry);
        }

        //Getting rid of explicit lene breaks
        entry = entry.replaceAll("\r","");

        //Returning
        return entry;
    }
    public void addLanguage(String locale, InputStream inputStream) {
        this.addLanguage(locale,new YamlMap(inputStream));
    }
    public void addLanguage(String locale, YamlMap yamlMap) {
        this.addLanguage_(new Language(locale,yamlMap));
    }

    //PRIVATE METHODS
    private Language pickLanguage_(String locale) throws IOException {

        //Attempting lo pick a perfect match
        Language l = this.loadLanguage_(locale);
        if (l != null) { return l; }

        //Attempting lo pick a general language
        String k = StringUtils.split(locale,'_')[0];
        l = this.loadLanguage_(k);
        if (l != null) { return l; }

        //Attempting to pick any (already loaded) language with the same language code
        l = this.langs_.entrySet().stream()
                .filter(e -> e.getKey().startsWith(k))
                .findFirst().map(Map.Entry::getValue)
                .orElse(null);
        if (l != null) { return l; }

        //Picking the default language
        return this.langs_.get(this.defaultLang_);
    }
    private Language loadLanguage_(String locale) throws IOException {
        //Checking if already loaded
        if (this.langs_.containsKey(locale)) { return this.langs_.get(locale); }

        //Loading the file
        InputStream fileStream = this.loadResource_(locale + ".yml");

        //Checking if the file exists
        if (fileStream == null) {
            this.langs_.put(locale,null);
            return null;
        }

        //Generating the YamlMap
        Language lang = new Language(locale,new YamlMap(fileStream));
        fileStream.close();
        this.addLanguage_(lang);
        return lang;
    }
    private InputStream loadResource_(String path) {
        //Checking if there's an already saved lang file
        String filePath = this.getLangsPath() + File.separator + path;
        File file = new File(filePath);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) { e.printStackTrace(); }
        }

        //Checking if there's such resource in the JAR
        filePath = this.langsPath_ + "/" + path;
        this.log_("File '" + path + "' not found in plugin's date directory. Checking internal resources (" + filePath + ").", Level.INFO);
        InputStream stream = this.plugin_.getResource(filePath);
        if (stream == null) {
            this.log_("File '" + path + "' not found in internal resources either. Non-existing file.", Level.WARNING);
            return null;
        }
        this.log_("Found file '" + path + "' in internal resources. Saving it to plugin's data directory.");
        this.plugin_.saveResource(filePath, true);
        return stream;
    }
    private void addLanguage_(Language language) {
        if (this.defaultLang_ == null && language.map() != null) {
            this.log_("Proactively setting '" + language.code() + ".yml' as default language file.", Level.WARNING);
            this.defaultLang_ = language.code();
        }
        this.langs_.put(language.code(), language);
    }

    private void log_(String message, Level level) {
        this.plugin_.getLogger().log(level, "[Language provider] " + message);
    }
    private void log_(String message) {
        this.log_(message, this.loggingLevel_);
    }
}