import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "agent-service"))

from tools.runtime_context import is_runtime_identity_query, order_status


class RuntimeContextTest(unittest.TestCase):
    def test_identity_query_detection(self):
        self.assertTrue(is_runtime_identity_query("我是谁？"))
        self.assertFalse(is_runtime_identity_query("查一下订单物流"))

    def test_order_status_normalization(self):
        self.assertEqual("PENDING_SHIPMENT", order_status({"status": "PAID_PENDING_SHIPMENT"}))
        self.assertEqual("SHIPPED", order_status({"status": "SHIPPED"}))


if __name__ == "__main__":
    unittest.main()