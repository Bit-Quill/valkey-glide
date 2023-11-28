using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace netty_log_parser
{
	internal struct TestData
	{
		public List<long> java2rust_times = new ();
		public List<long> rust_request_times = new ();
		public List<long> redis_times = new();
		public List<long> rust_response_times = new();
		public List<long> rust2java_times = new ();

		public long total = 0;
		public long waiting = 0;
		public long writes = 0;
		public long reads = 0;
		public long errors = 0;
		public long write_submit_request = 0;
		public long write_submit_to_build = 0;
		public long write_build_command = 0;
		public long write_submit_command = 0;
		public long write_handling = 0;
		public long write_buffer = 0;
		public long write_serialize = 0;
		public long write_submit = 0;
		public long write_total_outbound = 0;
		public long read_buffer = 0;
		public long read_parse = 0;
		public long read_set_future = 0;
		public long read_total_inbound = 0;
		public long read_set_to_resolve = 0;
		public long read_resolve = 0;
		public long read_resolve_to_get = 0;
		public long avg_resp_time = 0;
		public string name;

		public TestData()
		{
		}
	}


	internal class Program
	{
		private static (TestData?, int) ParseTestData(string[] lines, int start)
		{
			var dt_pattern = @"(\d+\-\d+\-\d+[T ]\d+:\d+:\d+\.\d+Z?)";
			var dt_re = new Regex(dt_pattern, RegexOptions.Compiled);

			var test_name_re = new Regex(@"==== ([\w\s\d]+) ====", RegexOptions.Compiled);
			var param_re = new Regex(@"\->[\w:\s]+\s+(\d+)", RegexOptions.Compiled);

			var test_start = "++++ START OF TEST ++++";
			var test_end = "++++ END OF TEST ++++";

			DateTime? prevTs = null;

			var data = new TestData();

			bool first_line_found = false;
			int i = start;

			for (; i < lines.Length; i++)
			{
				var line = lines[i];

				if (!first_line_found && line != test_start)
					continue;
				if (!first_line_found && line == test_start)
				{
					first_line_found = true;
					continue;
				}

				if (line.Contains("sending callback id"))
				{
					prevTs = DateTime.Parse(dt_re.Match(line).Groups[0].Value);
				}
				else if (line.Contains("callback id sent     -"))
				{
					var ts = DateTime.Parse(dt_re.Match(line).Groups[0].Value);
					data.rust_response_times.Add((long)(ts - prevTs.Value).TotalNanoseconds);
					prevTs = ts;
				}
				else if (line.Contains("callback id received -"))
				{
					var ts = DateTime.Parse(dt_re.Match(line).Groups[0].Value);
					data.java2rust_times.Add((long)(ts - prevTs.Value).TotalNanoseconds);
					prevTs = ts;
				}
				else if (line.Contains("received callback id"))
				{
					var ts = DateTime.Parse(dt_re.Match(line).Groups[0].Value);
					data.rust2java_times.Add((long)(ts - prevTs.Value).TotalNanoseconds);
				}
				else if (line.Contains("StandaloneClient - sending command"))
				{
					var ts = DateTime.Parse(dt_re.Match(line).Groups[0].Value);
					data.rust_request_times.Add((long)(ts - prevTs).Value.TotalNanoseconds);
					prevTs = ts;
				}
				else if (line.Contains("StandaloneClient - received response"))
				{
					var ts = DateTime.Parse(dt_re.Match(line).Groups[0].Value);
					data.redis_times.Add((long)(ts - prevTs.Value).TotalNanoseconds);
					prevTs = ts;
				}
				else if (test_name_re.IsMatch(line))
					data.name = test_name_re.Match(line).Groups[1].Value;
				else if (line.Contains("total:"))
					data.total = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("waiting:"))
					data.waiting = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("error count:"))
					data.errors = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("count writes"))
					data.writes = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("count reads"))
					data.reads = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("write: submit request"))
					data.write_submit_request = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("write: submit 2 build"))
					data.write_submit_to_build = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("build command"))
					data.write_build_command = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("submit command"))
					data.write_submit_command = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("handling"))
					data.write_handling = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("write: buffer"))
					data.write_buffer = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("serialize"))
					data.write_serialize = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("write: submit"))
					data.write_submit = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("write: total outbound"))
					data.write_total_outbound = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("read: buffer"))
					data.read_buffer = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("parse"))
					data.read_parse = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("set future res"))
					data.read_set_future = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("read: total inbound"))
					data.read_total_inbound = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("read: set to resolve"))
					data.read_set_to_resolve = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("read: resolve ptr"))
					data.read_resolve = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("read: resolve to get"))
					data.read_resolve_to_get = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("avg response time"))
					data.avg_resp_time = long.Parse(param_re.Match(line).Groups[1].Value);


				if (line == test_end)
					break;
			}

			if (!first_line_found)
				return (null, i);
			return (data, i);
		}

		private readonly static string report_template = File.ReadAllText(Path.Combine(Directory.GetCurrentDirectory(), "report_sample_ascii.txt"));

		private static string PrepareReport()
		{
			var report = report_template;
			int pos = 0;
			int index = 0;
			while (true)
			{
				int nextPercent = report.IndexOf("%%%", pos);
				int nextNumber = report.IndexOf("xxx", pos);
				if (((nextPercent < nextNumber && nextNumber > 0) || nextNumber < 0) && nextPercent > 0)
				{
					report = report.Substring(pos, nextPercent) + $"{{{index++}:00.00}}" + report.Substring(nextPercent + 5);
					continue;
				}
				if (((nextNumber < nextPercent && nextPercent > 0) || nextPercent < 0) && nextNumber > 0)
				{
					int fieldLen = report.IndexOf(' ', nextNumber) - nextNumber;
					report = report.Substring(pos, nextNumber) + $"{{{index++},{fieldLen}}}" + report.Substring(nextNumber + fieldLen);
					continue;
				}
				break;
			}

			return report;
		}

		public static void Main(string[] args)
		{
			var report_pattern = PrepareReport();

			var lines = File.ReadAllLines(args.Last());

			int pos = 0;

			while (true)
			{
				(var parsed, pos) = ParseTestData(lines, pos);
				if (parsed == null) break;

				var data = parsed.Value;

				var write_duration = data.write_build_command + data.write_submit_command + data.write_handling + data.write_serialize + data.write_buffer + data.write_submit + data.java2rust_times.Sum() + data.rust_request_times.Sum();
				var read_duration = data.read_resolve_to_get + data.read_resolve + data.read_set_to_resolve + data.read_set_future + data.read_parse + data.read_buffer + data.rust2java_times.Sum() + data.rust_response_times.Sum();
				var java_duration = data.write_build_command + data.write_submit_command + data.write_handling + data.write_serialize + data.write_buffer + data.write_submit
						+ data.read_resolve_to_get + data.read_resolve + data.read_set_to_resolve + data.read_set_future + data.read_parse + data.read_buffer;

                Console.WriteLine($"\n\n========= {data.name} ======== \n\n iterations {data.reads}\n total time {data.total:#,###}\n write time {write_duration:#,###}\n read time  {read_duration:#,###}\n errors: {data.errors}\n java time  {java_duration:#,###}\n non-java   {data.avg_resp_time * data.reads:#,###}\n\n");

				var report = string.Format(report_pattern,
					// % total
					data.write_submit_request * 100.0 / data.total,
					data.write_submit_to_build * 100.0 / data.total,
					data.write_build_command * 100.0 / data.total,
					data.write_submit_command * 100.0 / data.total,
					data.write_handling * 100.0 / data.total,
					data.write_serialize * 100.0 / data.total,
					data.write_buffer * 100.0 / data.total,
					data.write_submit * 100.0 / data.total,
					data.java2rust_times.Sum() * 100.0 / data.total,
					data.rust_request_times.Sum() * 100.0 / data.total,
					// % step
					data.write_build_command * 100.0 / (data.write_build_command + data.write_submit_command),
					data.write_submit_command * 100.0 / (data.write_build_command + data.write_submit_command),
					data.write_serialize * 100.0 / (data.write_serialize + data.write_buffer + data.write_submit),
					data.write_buffer * 100.0 / (data.write_serialize + data.write_buffer + data.write_submit),
					data.write_submit * 100.0 / (data.write_serialize + data.write_buffer + data.write_submit),
					// % write
					data.write_submit_request * 100.0 / write_duration,
					data.write_submit_to_build * 100.0 / write_duration,
					data.write_build_command * 100.0 / write_duration,
					data.write_submit_command * 100.0 / write_duration,
					data.write_handling * 100.0 / write_duration,
					data.write_serialize * 100.0 / write_duration,
					data.write_buffer * 100.0 / write_duration,
					data.write_submit * 100.0 / write_duration,
					data.java2rust_times.Sum() * 100.0 / write_duration,
					data.rust_request_times.Sum() * 100.0 / write_duration,
                    // % java
                    data.write_submit_request * 100.0 / java_duration,
                    data.write_submit_to_build * 100.0 / java_duration,
                    data.write_build_command * 100.0 / java_duration,
                    data.write_submit_command * 100.0 / java_duration,
                    data.write_handling * 100.0 / java_duration,
                    data.write_serialize * 100.0 / java_duration,
                    data.write_buffer * 100.0 / java_duration,
                    data.write_submit * 100.0 / java_duration,
                    // avg duration
                    data.write_submit_request / data.writes,
					data.write_submit_to_build / data.writes,
					data.write_build_command / data.writes,
					data.write_submit_command / data.writes,
					data.write_handling / data.writes,
					data.write_serialize / data.writes,
					data.write_buffer / data.writes,
					data.write_submit / data.writes,
					data.java2rust_times.Average(),
					data.rust_request_times.Average(),
					// stats
					data.waiting * 100.0 / data.total,
					data.avg_resp_time * data.reads * 100.0 / data.total,
					data.waiting,
					data.avg_resp_time * data.reads,
					// % total
					data.read_resolve_to_get * 100.0 / data.total,
					data.read_resolve * 100.0 / data.total,
					data.read_set_to_resolve * 100.0 / data.total,
					data.read_set_future * 100.0 / data.total,
					data.read_parse * 100.0 / data.total,
					data.read_buffer * 100.0 / data.total,
					data.rust2java_times.Sum() * 100.0 / data.total,
					data.rust_response_times.Sum() * 100.0 / data.total,
					data.redis_times.Sum() * 100.0 / data.total,
					// % step
					data.read_set_future * 100.0 / (data.read_set_future + data.read_parse + data.read_buffer),
					data.read_parse * 100.0 / (data.read_set_future + data.read_parse + data.read_buffer),
					data.read_buffer * 100.0 / (data.read_set_future + data.read_parse + data.read_buffer),
					// % read
					data.read_resolve_to_get * 100.0 / read_duration,
					data.read_resolve * 100.0 / read_duration,
					data.read_set_to_resolve * 100.0 / read_duration,
					data.read_set_future * 100.0 / read_duration,
					data.read_parse * 100.0 / read_duration,
					data.read_buffer * 100.0 / read_duration,
					data.rust2java_times.Sum() * 100.0 / read_duration,
					data.rust_response_times.Sum() * 100.0 / read_duration,
					// % java
					data.read_resolve_to_get * 100.0 / java_duration,
					data.read_resolve * 100.0 / java_duration,
					data.read_set_to_resolve * 100.0 / java_duration,
					data.read_set_future * 100.0 / java_duration,
					data.read_parse * 100.0 / java_duration,
					data.read_buffer * 100.0 / java_duration,
					// avg duration
					data.read_resolve_to_get / data.reads,
					data.read_resolve / data.reads,
					data.read_set_to_resolve / data.reads,
					data.read_set_future / data.reads,
					data.read_parse / data.reads,
					data.read_buffer / data.reads,
					data.rust2java_times.Sum() / data.reads,
					data.rust_response_times.Average(),
					data.redis_times.Average()
				);

				Console.WriteLine(report);

			}
		}
	}
}
