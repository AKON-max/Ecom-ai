package com.ecomai.ecommerce.service;

import com.ecomai.ecommerce.dto.LogisticsEventResponse;
import com.ecomai.ecommerce.dto.LogisticsResponse;
import com.ecomai.ecommerce.dto.OrderItemResponse;
import com.ecomai.ecommerce.dto.OrderResponse;
import com.ecomai.ecommerce.entity.LogisticsInfo;
import com.ecomai.ecommerce.entity.OrderEntity;
import com.ecomai.ecommerce.entity.OrderItem;
import com.ecomai.ecommerce.repository.LogisticsEventRepository;
import com.ecomai.ecommerce.repository.LogisticsInfoRepository;
import com.ecomai.ecommerce.repository.OrderItemRepository;
import com.ecomai.ecommerce.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final LogisticsInfoRepository logisticsInfoRepository;
    private final LogisticsEventRepository logisticsEventRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        LogisticsInfoRepository logisticsInfoRepository,
                        LogisticsEventRepository logisticsEventRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.logisticsInfoRepository = logisticsInfoRepository;
        this.logisticsEventRepository = logisticsEventRepository;
    }

    public OrderResponse getOrder(String orderNo, String currentUserId) {
        OrderEntity order = findOwnedOrder(orderNo, currentUserId);
        List<OrderItemResponse> items = orderItemRepository.findByOrderEntity(order).stream()
            .map(item -> new OrderItemResponse(item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice()))
            .collect(Collectors.toList());
        return new OrderResponse(order.getOrderNo(), order.getCustomerName(), order.getStatus(), order.getPaymentStatus(),
            order.getTotalAmount(), order.getCreatedAt(), order.getUserId(), order.getShippedAt(),
            order.getDeliveredAt(), order.getHasAfterSaleRequest(), order.getCancelAllowed(), items);
    }

    public LogisticsResponse getLogistics(String orderNo, String currentUserId) {
        OrderEntity order = findOwnedOrder(orderNo, currentUserId);
        LogisticsInfo info = logisticsInfoRepository.findByOrderEntity(order)
            .orElseThrow(() -> new IllegalArgumentException("Logistics not found for order: " + orderNo));
        List<LogisticsEventResponse> events = logisticsEventRepository.findByLogisticsInfoOrderByOccurredAtDesc(info).stream()
            .map(event -> new LogisticsEventResponse(event.getOccurredAt(), event.getContent()))
            .collect(Collectors.toList());
        return new LogisticsResponse(info.getCompany(), info.getTrackingNo(), info.getStatus(), info.getEstimatedDelivery(),
            info.getLatestUpdate(), info.getDeliveredAt(), info.getExceptionReason(), events);
    }

    private OrderEntity findOwnedOrder(String orderNo, String currentUserId) {
        OrderEntity order = orderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNo));
        // 服务重点：订单号只用于定位业务对象，授权必须由可信 Runtime Context 身份决定。
        if (!order.getUserId().equals(currentUserId)) {
            throw BusinessException.forbidden("ORDER_ACCESS_DENIED", "只能访问或处理当前用户自己的订单");
        }
        return order;
    }
}
