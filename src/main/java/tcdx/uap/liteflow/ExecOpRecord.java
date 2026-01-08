package tcdx.uap.liteflow;

public class ExecOpRecord {
    public String op_id;        // 如果是提交，则为-1
    public String op_type;      // submit insert update complete
    public String op_db_type;
    public String table_id;
    public int id_;
    public String node_;
    public String edge_;
    public String prev_node_;
    public String before_or_after; // before after

    public ExecOpRecord(String op_id, String op_type, String table_id, String before_or_after, int id_, String node_, String edge_, String prev_node_) {
        this.op_id = op_id;
        this.op_type = op_type;
        this.table_id = table_id;
        this.before_or_after = before_or_after;
        this.id_ = id_;
        this.node_ = node_;
        this.edge_ = edge_;
        this.prev_node_ = prev_node_;
    }
}