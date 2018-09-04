package net.landzero.xlog.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.FilterReply;
import net.landzero.xlog.utils.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantLock;

public abstract class XLogBaseAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    public static final String TOPIC_JSON = "_json_";

    public static final String MODE_PLAIN = "plain";

    public static final String MODE_JSON = "json";

    public static final String PLAIN_LAYOUT = "[%d{yyyy/MM/dd HH:mm:ss.SSS}] %X{cridMark} [%thread] %-5level %logger{35} - %msg%n";

    public static final String JSON_LAYOUT = "[%d{yyyy/MM/dd HH:mm:ss.SSS}] %msg%n";

    /**
     * All synchronization in this class is done via the lock object.
     */

    protected final ReentrantLock lock = new ReentrantLock(false);

    private String mode = MODE_PLAIN;

    private PatternLayout layout = null;

    private String project = null;

    private String topic = null;

    private String env = null;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isJsonMode() {
        if (this.mode == null)
            return false;
        return this.mode.trim().equalsIgnoreCase(MODE_JSON);
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = Strings.normalize(project);
    }

    public String getTopic() {
        if (isJsonMode()) {
            return TOPIC_JSON;
        }
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = Strings.normalize(topic);
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = Strings.normalize(env);
    }

    /**
     * use easy syntax to add a common logback filter
     * <p>
     * 使用简洁的语法快速添加常见的日志过滤器
     * <p>
     * example:
     *
     * <code>info</code> == LevelFilter (level INFO, onMatch ACCEPT, onMismatch DENY)
     * <code>-info</code> == LevelFilter (level INFO, onMatch DENY, onMismatch ACCEPT)
     * <code>info+</code> == ThresholdFilter (level INFO)
     *
     * @param easyFilter easy filter syntax
     */
    public void addEasyFilter(String easyFilter) {
        easyFilter = Strings.normalize(easyFilter);
        if (easyFilter == null) {
            return;
        }
        easyFilter = easyFilter.toUpperCase();
        if (easyFilter.endsWith("+")) {
            ThresholdFilter filter = new ThresholdFilter();
            filter.setLevel(easyFilter.substring(0, easyFilter.length() - 1));
            addFilter(filter);
        } else if (easyFilter.startsWith("-")) {
            LevelFilter filter = new LevelFilter();
            filter.setLevel(Level.toLevel(easyFilter.substring(1)));
            filter.setOnMatch(FilterReply.DENY);
            filter.setOnMismatch(FilterReply.ACCEPT);
            addFilter(filter);
        } else {
            LevelFilter filter = new LevelFilter();
            filter.setLevel(Level.toLevel(easyFilter));
            filter.setOnMatch(FilterReply.ACCEPT);
            filter.setOnMismatch(FilterReply.DENY);
            addFilter(filter);
        }
    }

    protected void initLayout() {
        closeLayout();
        if (this.layout == null) {
            this.layout = new PatternLayout();
            this.layout.setPattern(isJsonMode() ? JSON_LAYOUT : PLAIN_LAYOUT);
            this.layout.setContext(getContext());
            this.layout.start();
            if (!this.layout.isStarted()) {
                closeLayout();
            }
        }
    }

    protected void closeLayout() {
        if (this.layout != null) {
            this.layout.stop();
            this.layout = null;
        }
    }

    @Override
    public void start() {
        initLayout();
        if (this.layout == null) {
            addError("layout failed to start");
        } else {
            super.start();
        }
    }

    @Override
    public void stop() {
        closeLayout();
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        appendString(this.layout.doLayout(eventObject));
    }

    abstract protected void appendString(@NotNull String string);

}
