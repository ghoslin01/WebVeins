package com.xiongbeer.webveins.filter.bloom;

/**
 * Created by shaoxiong on 17-4-18.
 */
public class BloomTable {
    protected long size;
    protected int[] seeds;
    protected AbstractBitSet bits;
    protected HashMaker[] func;

    public BloomTable(AbstractBitSet bits, long size, int[] seeds){
        this.bits = bits;
        this.size = size;
        this.seeds = seeds;
        func = new HashMaker[seeds.length];
        for(int i=0; i<seeds.length; ++i){
            func[i] = new HashMaker(size, seeds[i]);
        }
    }

    public boolean exist(String url) {
        if(url == null){
            return false;
        }
        boolean ret = true;
        for(HashMaker f:func){
            ret = ret && bits.get(f.getHash(url));
        }
        return ret;
    }

    public boolean add(String url) {
        if(url != null) {
            if(!exist(url)) {
                for (HashMaker f : func) {
                    bits.set(f.getHash(url), true);
                }
                return true;
            }
        }
        return false;
    }

    public boolean get(long index){
        return bits.get(index);
    }

}