package pl.sk.ocr.domain.identifier;

public record PageNumber(int value) implements Comparable<PageNumber> {
    public PageNumber {
        if (value < 1) {
            throw new IllegalArgumentException("page number must be greater than or equal to 1");
        }
    }

    @Override
    public int compareTo(PageNumber other) {
        return Integer.compare(value, other.value);
    }
}
