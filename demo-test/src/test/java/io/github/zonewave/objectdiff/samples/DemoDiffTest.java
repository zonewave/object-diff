package io.github.zonewave.objectdiff.samples;

import static org.junit.jupiter.api.Assertions.*;

import io.github.zonewave.objectdiff.samples.Demo.*;
import org.junit.jupiter.api.Test;

class DemoDiffTest {


    @Test
    void testDiffDemo(){
        var d1=new Demo(1,"2",new DemoInner(3),"4",true,true,true);
        var d2=new Demo(11,"22",new DemoInner(5),"44",false,false ,false);
        var changes=DemoDiff.INSTANCE.diffDemo(d1,d2);
        assertEquals(7,changes.size());
    }
}