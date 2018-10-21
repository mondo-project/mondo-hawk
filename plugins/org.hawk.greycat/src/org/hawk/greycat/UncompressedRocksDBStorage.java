/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawk.greycat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import greycat.Callback;
import greycat.Constants;
import greycat.Graph;
import greycat.plugin.Storage;
import greycat.rocksdb.LibraryLoader;
import greycat.rocksdb.RocksDBStorage;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.utility.HashHelper;


/**
 * This is a copy of the {@link RocksDBStorage} class from the official Greycat RocksDB backend,
 * changing the default connection options so no compression will be used. This is needed to have
 * it work in Windows, since the default RocksJava distribution does not include Snappy in the
 * Windows DLL.
 * 
 * This class will be removed if the Greycat authors provide a way to enable/disable the
 * compression used in the engine.
 * 
 * More details here:
 * 
 * https://github.com/datathings/greycat/issues/94
 */
public class UncompressedRocksDBStorage implements Storage {

    private Options _options;

    private RocksDB _db;

    private Graph _graph;
    private final List<Callback<Buffer>> updates = new ArrayList<Callback<Buffer>>();

    private static final String _connectedError = "PLEASE CONNECT YOUR DATABASE FIRST";

    private boolean _isConnected = false;

    private final String _storagePath;

    public UncompressedRocksDBStorage(String storagePath) {
        if (System.getProperty("os.arch").equals("arm")) {
            LibraryLoader.loadArmLibrary("librocksdbjni-linux32");
        }
        RocksDB.loadLibrary();
        this._storagePath = storagePath;
    }

    @Override
    public void listen(Callback<Buffer> synCallback) {
        updates.add(synCallback);
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        final Buffer result = _graph.newBuffer();
        final BufferIterator it = keys.iterator();
        /*
        List<byte[]> query = new ArrayList<byte[]>();
        while (it.hasNext()) {
            Buffer view = it.next();
            query.add(view.data());
        }
        try {
            Map<byte[], byte[]> dbResult = _db.multiGet(query);
            boolean isFirst = true;
            for (int i = 0; i < query.size(); i++) {
                if (!isFirst) {
                    result.write(Constants.BUFFER_SEP);
                } else {
                    isFirst = false;
                }
                byte[] subResult = dbResult.get(query.get(i));
                if (subResult != null) {
                    result.writeAll(subResult);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (callback != null) {
            callback.on(result);
        }
        */

        boolean isFirst = true;
        while (it.hasNext()) {
            Buffer view = it.next();
            try {
                if (!isFirst) {
                    result.write(Constants.BUFFER_SEP);
                } else {
                    isFirst = false;
                }
                byte[] res = _db.get(view.data());
                if (res != null) {
                    result.writeAll(res);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (callback != null) {
            callback.on(result);
        }

    }

    @Override
    public void put(Buffer stream, Callback<Boolean> p_callback) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        Buffer result = null;
        if (updates.size() != 0) {
            result = _graph.newBuffer();
        }
        WriteBatch batch = new WriteBatch();
        BufferIterator it = stream.iterator();
        boolean isFirst = true;
        while (it.hasNext()) {
            Buffer keyView = it.next();
            Buffer valueView = it.next();
            if (valueView != null) {
                batch.put(keyView.data(), valueView.data());
            }
            if (result != null) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    result.write(Constants.KEY_SEP);
                }
                result.writeAll(keyView.data());
                result.write(Constants.KEY_SEP);
                Base64.encodeLongToBuffer(HashHelper.hashBuffer(valueView, 0, valueView.length()), result);
            }
        }
        WriteOptions options = new WriteOptions();
        options.setSync(false);
        try {
            _db.write(options, batch);
            for (int i = 0; i < updates.size(); i++) {
                final Callback<Buffer> explicit = updates.get(i);
                explicit.on(result);
            }
            if (p_callback != null) {
                p_callback.on(true);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
            if (p_callback != null) {
                p_callback.on(false);
            }
        }
    }

    @Override
    public final void putSilent(Buffer stream, Callback<Buffer> callback) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        Buffer result = _graph.newBuffer();
        WriteBatch batch = new WriteBatch();
        BufferIterator it = stream.iterator();
        boolean isFirst = true;
        while (it.hasNext()) {
            Buffer keyView = it.next();
            Buffer valueView = it.next();
            if (valueView != null) {
                batch.put(keyView.data(), valueView.data());
            }
            if (isFirst) {
                isFirst = false;
            } else {
                result.write(Constants.KEY_SEP);
            }
            result.writeAll(keyView.data());
            result.write(Constants.KEY_SEP);
            Base64.encodeLongToBuffer(HashHelper.hashBuffer(valueView, 0, valueView.length()), result);
        }
        WriteOptions options = new WriteOptions();
        options.setSync(false);
        try {
            _db.write(options, batch);
            for (int i = 0; i < updates.size(); i++) {
                final Callback<Buffer> explicit = updates.get(i);
                explicit.on(result);
            }
            callback.on(result);
        } catch (RocksDBException e) {
            e.printStackTrace();
            callback.on(null);
        }
    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            BufferIterator it = keys.iterator();
            while (it.hasNext()) {
                Buffer view = it.next();
                _db.delete(view.data());
            }
            if (callback != null) {
                callback.on(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        //TODO write the prefix
        try {
            WriteOptions options = new WriteOptions();
            options.sync();
            _db.write(options, new WriteBatch());
            _db.close();
            _options.close();
            _options = null;
            _db = null;
            _isConnected = false;
            if (callback != null) {
                callback.on(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }

    private static final byte[] prefixKey = "prefix".getBytes();

    @SuppressWarnings("resource")
	@Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        if (_isConnected) {
            if (callback != null) {
                callback.on(true);
            }
            return;
        }
        _graph = graph;
        //by default activate snappy compression of bytes
        _options = new Options()
                .setCreateIfMissing(true);
        File location = new File(_storagePath);
        if (!location.exists()) {
            location.mkdirs();
        }
        File targetDB = new File(location, "data");
        targetDB.mkdirs();
        try {
            _db = RocksDB.open(_options, targetDB.getAbsolutePath());
            _isConnected = true;
            if (callback != null) {
                callback.on(true);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }

    @Override
    public void lock(Callback<Buffer> callback) {
        try {
            byte[] current = _db.get(prefixKey);
            if (current == null) {
                current = new String("0").getBytes();
            }
            Short currentPrefix = Short.parseShort(new String(current));
            _db.put(prefixKey, ((currentPrefix + 1) + "").getBytes());
            if (callback != null) {
                Buffer newBuf = _graph.newBuffer();
                Base64.encodeIntToBuffer(currentPrefix, newBuf);
                callback.on(newBuf);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(null);
            }
        }
    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        //noop
        callback.on(true);
    }
}
