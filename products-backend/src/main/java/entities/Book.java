package entities;

import java.util.Comparator;
import java.util.Objects;
import java.util.Random;

import com.alibaba.fastjson.annotation.JSONField;

public final class Book {
    @JSONField(name = "book_id", ordinal = 1)
    private int bookId;
    @JSONField(name = "category", ordinal = 2)
    private String category;
    @JSONField(name = "title", ordinal = 3)
    private String title;
    @JSONField(name = "press", ordinal = 4)
    private String press;
    @JSONField(name = "publish_year", ordinal = 5)
    private int publishYear;
    @JSONField(name = "author", ordinal = 6)
    private String author;
    @JSONField(name = "price", ordinal = 7)
    private double price;
    @JSONField(name = "stock", ordinal = 8)
    private int stock;

    public enum SortColumn {
        BOOK_ID("book_id", Comparator.comparingInt(Book::getBookId)),
        CATEGORY("category", Comparator.comparing(Book::getCategory)),
        TITLE("title", Comparator.comparing(Book::getTitle)),
        PRESS("press", Comparator.comparing(Book::getPress)),
        PUBLISH_YEAR("publish_year", Comparator.comparingInt(Book::getPublishYear)),
        AUTHOR("author", Comparator.comparing(Book::getAuthor)),
        PRICE("price", Comparator.comparingDouble(Book::getPrice)),
        STOCK("stock", Comparator.comparingInt(Book::getStock));

        private final String value;
        private final Comparator<Book> comparator;

        public String getValue() {
            return value;
        }

        public Comparator<Book> getComparator() {
            return comparator;
        }

        SortColumn(String value, Comparator<Book> comparator) {
            this.value = value;
            this.comparator = comparator;
        }

        public static SortColumn random() {
            return values()[new Random().nextInt(values().length)];
        }
    }

    public Book() {
    }

    public Book(String category, String title, String press, int publishYear,
                String author, double price, int stock) {
        this.category = category;
        this.title = title;
        this.press = press;
        this.publishYear = publishYear;
        this.author = author;
        this.price = price;
        this.stock = stock;
    }

    @Override
    public Book clone() {
        Book b = new Book(category, title, press, publishYear, author, price, stock);
        b.bookId = bookId;
        return b;
    }

    @Override
    public String toString() {
        return "Book {" + "bookId=" + bookId +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                ", press='" + press + '\'' +
                ", publishYear=" + publishYear +
                ", author='" + author + '\'' +
                ", price=" + String.format("%.2f", price) +
                ", stock=" + stock +
                '}';
    }

    /* we assume that two books are equal iff their category...author are equal */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return publishYear == book.publishYear &&
                category.equals(book.category) &&
                title.equals(book.title) &&
                press.equals(book.press) &&
                author.equals(book.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, title, press, publishYear, author);
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPress() {
        return press;
    }

    public void setPress(String press) {
        this.press = press;
    }

    public int getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(int publishYear) {
        this.publishYear = publishYear;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
