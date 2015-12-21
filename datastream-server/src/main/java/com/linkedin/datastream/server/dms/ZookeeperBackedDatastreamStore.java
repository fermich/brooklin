package com.linkedin.datastream.server.dms;

import com.linkedin.datastream.common.Datastream;
import com.linkedin.datastream.common.DatastreamUtils;
import com.linkedin.datastream.common.zk.ZkClient;
import com.linkedin.datastream.server.zk.KeyBuilder;


public class ZookeeperBackedDatastreamStore implements DatastreamStore {

  private final ZkClient _zkClient;
  private final String _rootPath;

  public ZookeeperBackedDatastreamStore(ZkClient zkClient, String cluster) {
    assert zkClient != null;
    assert cluster != null;

    _zkClient = zkClient;
    _rootPath = KeyBuilder.datastreams(cluster);
  }

  private String getZnodePath(String key) {
    return _rootPath + "/" + key;
  }

  @Override
  public Datastream getDatastream(String key) {
    if (key == null) {
      return null;
    }
    String path = getZnodePath(key);
    String json = _zkClient.readData(path, true /* returnNullIfPathNotExists */);
    if (json == null) {
      return null;
    }
    return DatastreamUtils.fromJSON(json);
  }

  @Override
  public boolean updateDatastream(String key, Datastream datastream) {
    // Updating a Datastream is still tricky for now. Changing either the
    // the source or target may result in failure on connector.
    // We could possibly only allow updates on metadata field
    return false;
  }

  @Override
  public boolean createDatastream(String key, Datastream datastream) {
    if (key == null || datastream == null) {
      return false;
    }
    String path = getZnodePath(key);
    if (_zkClient.exists(path)) {
      return false;
    }
    _zkClient.ensurePath(path);
    String json = DatastreamUtils.toJSON(datastream);
    _zkClient.writeData(path, json);
    return true;
  }

  @Override
  public boolean deleteDatastream(String key) {
    if (key == null) {
      return false;
    }
    String path = getZnodePath(key);
    if (!_zkClient.exists(path)) {
      // delete operation is idempotent
      return true;
    }
    _zkClient.delete(getZnodePath(key));
    return true;
  }
}
