package queries;

import entities.Goods;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

public class GoodsResults {

    @JSONField(serialize = false)
    private int count;   /* number of results, equal to results.size() */
    private List<Goods> results;

    public GoodsResults(List<Goods> results) {
        this.count = results.size();
        this.results = results;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Goods> getResults() {
        return results;
    }

    public void setResults(List<Goods> results) {
        this.results = results;
    }
}
