import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class CapabilityContractTest(unittest.TestCase):
    def test_capability_manifest_has_core_controls(self):
        manifest = json.loads((ROOT / "agent-service" / "agent_capabilities.json").read_text(encoding="utf-8"))
        features = manifest["features"]
        for key in ("tool_calls", "rag_citations", "workflow", "human_approval", "trace", "evaluation", "cost_summary", "prompt_injection_defense"):
            self.assertTrue(features[key], key)

    def test_manifest_has_no_course_branding(self):
        text = (ROOT / "agent-service" / "agent_capabilities.json").read_text(encoding="utf-8").lower()
        self.assertNotIn("xiaozhe", text)
        self.assertNotIn("小哲", text)


if __name__ == "__main__":
    unittest.main()