package org.noear.solon.web.sse;

import org.noear.solon.core.Lifecycle;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.Context;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * web sse 简单实现
 *
 * @author kongweiguang
 * @since  2.3
 */
public class SseEmitter implements Lifecycle {
    private final Context ctx;
    private final LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
    private final AtomicBoolean b = new AtomicBoolean(false);

    private Runnable onCompletion;
    private Consumer<Throwable> onError;

    private final long keepAlive;


    /**
     * 完成之前回调方法
     *
     * @param onCompletion
     * @return
     */
    public SseEmitter onCompletion(Runnable onCompletion) {
        this.onCompletion = onCompletion;
        return this;
    }


    /**
     * 异常回调方法
     *
     * @param onError
     * @return
     */
    public SseEmitter onError(Consumer<Throwable> onError) {
        this.onError = onError;
        return this;
    }



    public SseEmitter(long keepAlive) {
        this.ctx = Context.current();

        if (keepAlive > 0) {
            this.keepAlive = keepAlive;
        } else {
            this.keepAlive = 60;
        }

        internalSendHeader();
    }

    /**
     * 发送事件内容
     *
     * @param data 事件数据
     */
    public void send(String data) throws IOException{
        send(new SseEvent().data(data));
    }

    /**
     * 发送事件内容
     *
     * @param event 事件数据
     */
    public void send(SseEvent event) throws IOException {
        if (event == null) {
            return;
        }

        if (b.get()) {
            return;
        }


        if (ctx.asyncSupported()) {
            internalSendEvent(event.build());
        } else {
            try {
                q.put(event.build());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * 发送任务
     */
    private void executeSendTask() throws Throwable {
        while (!b.get()) {
            String event = q.poll();
            if (event != null) {
                internalSendEvent(event);
            }

            Thread.sleep(1000L);
        }
    }

    /**
     * 设置头
     */
    private void internalSendHeader() {
        ctx.headerSet("Content-Type", "text/event-stream");
        ctx.headerSet("Cache-Control", "no-cache");
        ctx.headerSet("Connection", "keep-alive");
        ctx.headerSet("Keep-Alive", "timeout=" + keepAlive);
    }

    private void internalSendEvent(String event) throws IOException {
        ctx.output(event);
        ctx.flush();
    }


    /**
     * 任务开始
     */
    @Override
    public void start() throws Throwable {
        try {
            if(ctx.asyncSupported()){
                ctx.asyncStart();
            }else {
                executeSendTask();
            }
        } catch (Throwable e) {
            if (onError != null) {
                onError.accept(e);
            } else {
                EventBus.pushTry(e);
            }
        }
    }


    /**
     * 任务关闭
     */
    @Override
    public void stop() throws Throwable {
        if (onCompletion != null) {
            onCompletion.run();
        }

        b.set(true);
        ctx.asyncComplete();
    }
}
