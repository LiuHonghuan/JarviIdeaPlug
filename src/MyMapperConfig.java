/**
 * @author honghuan.Liu
 * @date 2022/9/3 21:42
 */
public class MyMapperConfig {

    private String namespace;

    private String keyword;

    public MyMapperConfig(String namespace, String keyword) {
        this.namespace = namespace;
        this.keyword = keyword;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
