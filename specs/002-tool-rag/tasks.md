# Tasks: Tool and RAG Authority

## Phase 1 · Tool Contract

- [x] T-TOOL-01 定义只读工具注册表
- [x] T-TOOL-02 实现订单号提取和 Runtime 归属解析
- [x] T-TOOL-03 实现缺参澄清和候选选择
- [x] T-TOOL-04 实现 E-commerce Client timeout 和错误分类
- [x] T-TOOL-05 实现 ToolCallTrace
- [x] T-TOOL-06 实现 Observation 压缩

## Phase 2 · Retrieval

- [x] T-RAG-01 加载知识文件和 metadata
- [x] T-RAG-02 实现 keyword retrieval
- [x] T-RAG-03 实现 dense retrieval
- [x] T-RAG-04 实现 RRF
- [x] T-RAG-05 实现轻量 rerank
- [x] T-RAG-06 实现 Citation contract
- [x] T-RAG-07 实现索引版本和缓存 key

## Phase 3 · Verification

- [x] T-TEST-01 物流工具 case
- [x] T-TEST-02 跨账号订单 case
- [x] T-TEST-03 商品 + 活动联合回答 case
- [x] T-TEST-04 低置信转人工 case
- [x] T-TEST-05 rerank fallback 测试
- [ ] T-PROD-01 接入生产级向量库
- [ ] T-PROD-02 接入 Cross-Encoder Reranker

## Traceability

| Requirement | Task | Verification |
|---|---|---|
| R-TOOL-01 | T-TOOL-01 | registry contract |
| R-TOOL-02 | T-TOOL-02 | cross-account |
| R-TOOL-03 | T-TOOL-03 | clarification |
| R-TOOL-04 | T-TOOL-01 | registry review |
| R-TOOL-05 | T-TOOL-05 | trace assertions |
| R-TOOL-06 | T-TOOL-04 | degradation |
| R-RAG-01 | T-RAG-01 | hybrid tests |
| R-RAG-02 | T-RAG-06 | citation |
| R-RAG-03 | T-RAG-07 | low confidence |
| R-RAG-04 | T-RAG-04 | fusion tests |
| R-RAG-05 | T-RAG-05 | fallback tests |
| R-RAG-06 | T-RAG-07 | offline mode |