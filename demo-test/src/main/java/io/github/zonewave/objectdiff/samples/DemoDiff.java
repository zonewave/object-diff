package io.github.zonewave.objectdiff.samples;

import io.github.zonewave.objectdiff.core.common.Change;
import io.github.zonewave.objectdiff.core.ifaces.Diff;
import java.util.Map;

@Diff
public interface DemoDiff {
    DemoDiff INSTANCE = new DemoDiffImpl();
    Map<String, Change> diffDemo(Demo oldObj, Demo newObj);
}
