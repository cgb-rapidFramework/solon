package org.noear.solon.boot.undertow.websocket;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.noear.solon.core.XContextEmpty;
import org.noear.solon.core.XMethod;
import org.noear.solonx.socket.api.XSocketMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

public class WsContext extends XContextEmpty {
    private InetSocketAddress _inetSocketAddress;
    private WebSocketChannel _socket;
    private XSocketMessage _message;
    private boolean _messageIsString;
    public WsContext(WebSocketChannel socket, XSocketMessage message, boolean messageIsString){
        _socket = socket;
        _message = message;
        _inetSocketAddress = socket.getSourceAddress();
        _messageIsString = messageIsString;
    }

    @Override
    public Object request() {
        return _socket;
    }

    @Override
    public String ip() {
        if(_inetSocketAddress == null)
            return null;
        else
            return _inetSocketAddress.getAddress().toString();
    }

    @Override
    public boolean isMultipart() {
        return false;
    }

    @Override
    public String method() {
        return XMethod.WEBSOCKET.name;
    }

    @Override
    public String protocol() {
        return "WS";
    }

    @Override
    public URI uri() {
        if(_uri == null) {
            _uri = URI.create(url());
        }

        return _uri;
    }
    private URI _uri;

    @Override
    public String path() {
        return uri().getPath();
    }



    @Override
    public String url() {
        return _message.resourceDescriptor;
    }

    @Override
    public long contentLength() {
        if (_message.content == null) {
            return 0;
        } else {
            return _message.content.length;
        }
    }

    @Override
    public String contentType() {
        return headerMap().get("Content-Type");
    }

    @Override
    public InputStream bodyAsStream() throws IOException {
        return new ByteArrayInputStream(_message.content);
    }

    //==============

    @Override
    public Object response() {
        return _socket;
    }

    @Override
    public void contentType(String contentType) {
        headerSet("Content-Type",contentType );
    }

    ByteArrayOutputStream _outputStream = new ByteArrayOutputStream();

    @Override
    public OutputStream outputStream() {
        return _outputStream;
    }

    @Override
    public void output(byte[] bytes)  {
        try {
            _outputStream.write(bytes);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void output(InputStream stream) {
        try {
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = stream.read(buff, 0, 100)) > 0) {
                _outputStream.write(buff, 0, rc);
            }

        }catch (Throwable ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void commit() throws IOException {
        if (_socket.isOpen()) {
            if (_messageIsString) {
                WebSockets.sendText(new String(_outputStream.toByteArray()), _socket, null);
            } else {
                ByteBuffer buf = ByteBuffer.wrap(_outputStream.toByteArray());
                WebSockets.sendBinary(buf, _socket, null);
            }
        }
    }

    @Override
    public void close() throws IOException {
        _socket.close();
    }
}
