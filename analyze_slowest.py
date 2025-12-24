#!/usr/bin/env python3
"""
分析 bvtlog 文件，找出最耗时的测试文件和目录
"""

import re
import sys
from collections import defaultdict
from pathlib import Path

def parse_log_file(log_file):
    """
    解析日志文件，提取文件路径和执行时间，区分 Executor 和 Executor2
    返回: {executor_name: (file_times, dir_times)}
    - file_times: {文件路径: 总耗时}
    - dir_times: {目录路径: 总耗时}
    """
    results = {
        'Executor': (defaultdict(float), defaultdict(float)),
        'Executor2': (defaultdict(float), defaultdict(float)),
    }
    
    # 匹配模式: "Executor:298 - The script file[...] has been executed, and cost: X.XXXs"
    pattern = r'(Executor2?):.*The script file\[([^\]]+)\] has been executed, and cost: ([\d.]+)s'
    
    with open(log_file, 'r', encoding='utf-8') as f:
        for line in f:
            match = re.search(pattern, line)
            if match:
                executor = match.group(1)
                file_path = match.group(2)
                cost_time = float(match.group(3))
                
                file_times, dir_times = results[executor]
                
                # 统计文件耗时
                file_times[file_path] += cost_time
                
                # 统计目录耗时
                dir_path = str(Path(file_path).parent)
                dir_times[dir_path] += cost_time
    
    return results

def format_time(seconds):
    """格式化时间显示"""
    if seconds >= 60:
        minutes = int(seconds // 60)
        secs = seconds % 60
        return f"{minutes}m {secs:.2f}s"
    else:
        return f"{seconds:.2f}s"

def print_top_items(items_dict, title, top_n=20):
    """打印最耗时的项目"""
    sorted_items = sorted(items_dict.items(), key=lambda x: x[1], reverse=True)
    
    print(f"\n{'='*80}")
    print(f"{title} (Top {top_n})")
    print(f"{'='*80}")
    print(f"{'排名':<6} {'耗时':<15} {'路径'}")
    print("-" * 80)
    
    for idx, (path, total_time) in enumerate(sorted_items[:top_n], 1):
        print(f"{idx:<6} {format_time(total_time):<15} {path}")
    
    if len(sorted_items) > top_n:
        print(f"\n... 还有 {len(sorted_items) - top_n} 个项目未显示")

def main():
    log_file = 'bvtlog'
    
    if len(sys.argv) > 1:
        log_file = sys.argv[1]
    
    if not Path(log_file).exists():
        print(f"错误: 日志文件 '{log_file}' 不存在")
        sys.exit(1)
    
    print(f"正在分析日志文件: {log_file}")
    print("解析中...")
    
    results = parse_log_file(log_file)
    
    print(f"\n解析完成!")
    
    # 分别统计 Executor 和 Executor2
    for executor_name in ['Executor', 'Executor2']:
        file_times, dir_times = results[executor_name]
        
        if not file_times:
            print(f"\n{executor_name}: 无数据")
            continue
        
        print(f"\n{'#'*80}")
        print(f"# {executor_name} 统计")
        print(f"{'#'*80}")
        print(f"共找到 {len(file_times)} 个文件, {len(dir_times)} 个目录")
        
        # 打印最耗时的文件
        print_top_items(file_times, f"{executor_name} - 最耗时的测试文件", top_n=30)
        
        # 打印最耗时的目录
        print_top_items(dir_times, f"{executor_name} - 最耗时的测试目录", top_n=30)
        
        # 统计信息
        total_file_time = sum(file_times.values())
        total_dir_time = sum(dir_times.values())
        
        print(f"\n{'='*80}")
        print(f"{executor_name} 统计摘要")
        print(f"{'='*80}")
        print(f"所有文件总耗时: {format_time(total_file_time)}")
        print(f"所有目录总耗时: {format_time(total_dir_time)}")
        print(f"平均文件耗时: {format_time(total_file_time / len(file_times) if file_times else 0)}")
        print(f"平均目录耗时: {format_time(total_dir_time / len(dir_times) if dir_times else 0)}")
    
    # 总体统计
    all_file_times = defaultdict(float)
    all_dir_times = defaultdict(float)
    for executor_name in ['Executor', 'Executor2']:
        file_times, dir_times = results[executor_name]
        for k, v in file_times.items():
            all_file_times[k] += v
        for k, v in dir_times.items():
            all_dir_times[k] += v
    
    total_time = sum(all_file_times.values())
    print(f"\n{'#'*80}")
    print(f"# 总体统计")
    print(f"{'#'*80}")
    print(f"总耗时: {format_time(total_time)}")

if __name__ == '__main__':
    main()

