package queries;

import entities.Card;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

public class CardList {

    @JSONField(serialize = false)
    private int count;
    private List<Card> cards;

    public CardList(List<Card> cards) {
        this.count = cards.size();
        this.cards = cards;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }
}
