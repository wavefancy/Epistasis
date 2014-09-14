package meshi.util.string;
import meshi.util.*;
import meshi.util.filters.*;;

public  class StringFilterEquals extends StringFilter {
    public  StringFilterEquals(String key) {super(key);}
    public  StringFilterEquals(StringList keys) {super(keys);}
    public boolean accept(String string,String key) {
	return string.equals(key);
    }
}
	
    
