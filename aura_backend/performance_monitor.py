#!/usr/bin/env python3
"""
Performance Monitor for AURA System
Tracks latency, accuracy, and optimization effectiveness
"""

import time
import logging
from typing import Dict, Any, List
from dataclasses import dataclass, field
from collections import defaultdict, deque
import statistics

logger = logging.getLogger(__name__)

@dataclass
class PerformanceMetrics:
    """Performance metrics for a single operation"""
    operation: str
    start_time: float
    end_time: float
    duration: float
    provider: str = ""
    model: str = ""
    success: bool = True
    confidence: float = 0.0
    cache_hit: bool = False
    instant_response: bool = False
    category: str = ""
    
    def __post_init__(self):
        if self.duration == 0:
            self.duration = self.end_time - self.start_time

class PerformanceMonitor:
    """Monitor and track AURA system performance"""
    
    def __init__(self, max_history: int = 1000):
        self.max_history = max_history
        self.metrics_history: deque = deque(maxlen=max_history)
        self.category_stats = defaultdict(list)
        self.provider_stats = defaultdict(list)
        self.total_requests = 0
        self.cache_hits = 0
        self.instant_responses = 0
        
    def record_operation(
        self, 
        operation: str,
        start_time: float,
        end_time: float = None,
        **kwargs
    ) -> PerformanceMetrics:
        """Record a completed operation"""
        if end_time is None:
            end_time = time.time()
            
        duration = end_time - start_time
        
        metrics = PerformanceMetrics(
            operation=operation,
            start_time=start_time,
            end_time=end_time,
            duration=duration,
            **kwargs
        )
        
        self.metrics_history.append(metrics)
        self.total_requests += 1
        
        if metrics.cache_hit:
            self.cache_hits += 1
        if metrics.instant_response:
            self.instant_responses += 1
            
        # Update category and provider stats
        if metrics.category:
            self.category_stats[metrics.category].append(duration)
        if metrics.provider:
            self.provider_stats[f"{metrics.provider}/{metrics.model}"].append(duration)
            
        return metrics
    
    def get_performance_summary(self) -> Dict[str, Any]:
        """Get comprehensive performance summary"""
        if not self.metrics_history:
            return {"error": "No performance data available"}
        
        recent_metrics = list(self.metrics_history)
        durations = [m.duration for m in recent_metrics]
        
        # Overall stats
        avg_latency = statistics.mean(durations) * 1000  # Convert to ms
        median_latency = statistics.median(durations) * 1000
        p95_latency = statistics.quantiles(durations, n=20)[18] * 1000 if len(durations) > 20 else max(durations) * 1000
        
        # Cache performance
        cache_hit_rate = (self.cache_hits / self.total_requests) * 100 if self.total_requests > 0 else 0
        instant_response_rate = (self.instant_responses / self.total_requests) * 100 if self.total_requests > 0 else 0
        
        # Success rate
        successful_ops = sum(1 for m in recent_metrics if m.success)
        success_rate = (successful_ops / len(recent_metrics)) * 100
        
        # Category performance
        category_performance = {}
        for category, times in self.category_stats.items():
            if times:
                category_performance[category] = {
                    "avg_latency_ms": statistics.mean(times) * 1000,
                    "count": len(times)
                }
        
        # Provider performance
        provider_performance = {}
        for provider, times in self.provider_stats.items():
            if times:
                provider_performance[provider] = {
                    "avg_latency_ms": statistics.mean(times) * 1000,
                    "count": len(times)
                }
        
        return {
            "overall": {
                "total_requests": self.total_requests,
                "avg_latency_ms": round(avg_latency, 2),
                "median_latency_ms": round(median_latency, 2),
                "p95_latency_ms": round(p95_latency, 2),
                "success_rate_percent": round(success_rate, 2),
                "cache_hit_rate_percent": round(cache_hit_rate, 2),
                "instant_response_rate_percent": round(instant_response_rate, 2)
            },
            "by_category": category_performance,
            "by_provider": provider_performance,
            "recent_operations": [
                {
                    "operation": m.operation,
                    "duration_ms": round(m.duration * 1000, 2),
                    "provider": f"{m.provider}/{m.model}" if m.provider else "unknown",
                    "category": m.category,
                    "cache_hit": m.cache_hit,
                    "instant": m.instant_response
                }
                for m in list(recent_metrics)[-10:]  # Last 10 operations
            ]
        }
    
    def get_optimization_recommendations(self) -> List[str]:
        """Get recommendations for performance optimization"""
        recommendations = []
        
        if not self.metrics_history:
            return ["No data available for recommendations"]
        
        recent_metrics = list(self.metrics_history)
        durations = [m.duration for m in recent_metrics]
        avg_latency = statistics.mean(durations) * 1000
        
        # High latency recommendations
        if avg_latency > 2000:  # 2 seconds
            recommendations.append("⚠️ High average latency detected. Consider optimizing prompts or switching to faster models.")
        
        # Cache recommendations
        cache_hit_rate = (self.cache_hits / self.total_requests) * 100 if self.total_requests > 0 else 0
        if cache_hit_rate < 20:
            recommendations.append("📈 Low cache hit rate. Consider expanding cache size or improving cache key strategy.")
        
        # Provider performance analysis
        provider_stats = {}
        for metrics in recent_metrics:
            if metrics.provider:
                key = f"{metrics.provider}/{metrics.model}"
                if key not in provider_stats:
                    provider_stats[key] = []
                provider_stats[key].append(metrics.duration)
        
        if len(provider_stats) > 1:
            best_provider = min(provider_stats.items(), key=lambda x: statistics.mean(x[1]))
            worst_provider = max(provider_stats.items(), key=lambda x: statistics.mean(x[1]))
            
            if statistics.mean(worst_provider[1]) > statistics.mean(best_provider[1]) * 2:
                recommendations.append(f"🚀 Consider using {best_provider[0]} more often (2x faster than {worst_provider[0]})")
        
        # Category-specific recommendations
        for category, times in self.category_stats.items():
            if times and statistics.mean(times) * 1000 > 1500:
                recommendations.append(f"🎯 {category} operations are slow. Consider optimizing prompts for this category.")
        
        if not recommendations:
            recommendations.append("✅ System performance is optimal!")
        
        return recommendations
    
    def log_performance_summary(self):
        """Log performance summary to logger"""
        summary = self.get_performance_summary()
        if "error" not in summary:
            overall = summary["overall"]
            logger.info(
                f"🚀 AURA Performance Summary: "
                f"Avg: {overall['avg_latency_ms']}ms, "
                f"P95: {overall['p95_latency_ms']}ms, "
                f"Success: {overall['success_rate_percent']}%, "
                f"Cache: {overall['cache_hit_rate_percent']}%, "
                f"Instant: {overall['instant_response_rate_percent']}%"
            )

# Global performance monitor instance
performance_monitor = PerformanceMonitor()
