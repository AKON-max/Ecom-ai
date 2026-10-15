# Tasks: Tool and RAG Authority

- [x] T-TOOL-01 定义只读工具清单
- [x] T-TOOL-02 实现 Runtime 订单归属校验
- [x] T-TOOL-03 实现缺参澄清和候选选择
- [x] T-TOOL-04 实现工具超时与错误分类
- [x] T-RAG-01 实现知识文件和 metadata 加载
- [x] T-RAG-02 实现 keyword + vector 混合召回
- [x] T-RAG-03 实现 RRF 候选融合
- [x] T-RAG-04 实现轻量 rerank
- [x] T-RAG-05 实现 citation contract
- [x] T-TEST-01 物流、商品、活动、低置信回归 case
- [ ] T-PROD-01 接入生产级向量库和索引切换

## Traceability

| Requirement | Test |
|---|---|
| R-TOOL-01 | cross-account case |
| R-TOOL-02 | clarification case |
| R-TOOL-03 | tool registry contract |
| R-TOOL-04 | tool trace assertions |
| R-RAG-01 | citation cases |
| R-RAG-02 | hybrid search tests |
| R-RAG-03 | rerank trace assertions |
| R-RAG-04 | low-confidence case |
| R-AUTH-01 | source guard tests |