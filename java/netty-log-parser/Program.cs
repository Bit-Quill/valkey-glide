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
		public long write_build_command = 0;
		public long write_submit_command = 0;
		public long write_handling = 0;
		public long write_buffer = 0;
		public long write_serialize = 0;
		public long write_submit = 0;
		public long write_total_netty = 0;
		public long read_buffer = 0;
		public long read_parse = 0;
		public long read_set_future = 0;
		public long read_total_netty = 0;
		public long read_get_future_res = 0;
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
				else if (line.Contains("count writes"))
					data.writes = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("count reads"))
					data.reads = long.Parse(param_re.Match(line).Groups[1].Value);
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
				else if (line.Contains("write: total netty"))
					data.write_total_netty = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("read: buffer"))
					data.read_buffer = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("parse"))
					data.read_parse = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("set future res"))
					data.read_set_future = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("read: total netty"))
					data.read_total_netty = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("get future res"))
					data.read_get_future_res = long.Parse(param_re.Match(line).Groups[1].Value);
				else if (line.Contains("avg response time"))
					data.avg_resp_time = long.Parse(param_re.Match(line).Groups[1].Value);


				if (line == test_end)
					break;
			}

			if (!first_line_found)
				return (null, i);
			return (data, i);
		}

        private readonly static string report_template = """
             /----------------------------\            /-------------------------------------------\   *******    /--------------\
             |             client         |            |                     netty                 |   *     *    |   babushka   |
             +---------------+------------+            +---------------+--------------+------------+   * UDS *    \--------------/
             | build command | submit cmd |            | serialization | buffer write |   submit   |   *     *    |              |
             \---------------+------------/            \---------------+--------------+------------/   *******    |              |
             |               |            |            |               |              |            |              |              |
% total      |<--- {0:00.00} --->|<-- {1:00.00} ->|<-- {2:00.00} ->|<--- {3:00.00} --->|<--- {4:00.00} -->|<-- {5:00.00} ->|<--- {6:00.00} -->|<--- {7:00.00} -->|< --...
% of step    |<--- {8:00.00} --->|<-- {9:00.00} ->|            |<--- {10:00.00} --->|<--- {11:00.00} -->|<-- {12:00.00} ->|              |              |
% of write   |<--- {13:00.00} --->|<-- {14:00.00} ->|<-- {15:00.00} ->|<--- {16:00.00} --->|<--- {17:00.00} -->|<-- {18:00.00} ->|<--- {19:00.00} -->|<--- {20:00.00} -->|
duration     |<-- {21,7} -->|<- {22,6} ->|<- {23,6} ->|<-- {24,6} --->|<-- {25,6} -->|<- {26,6} ->|<-- {27,6} -->|<-- {28,6} -->|< --...
             [======>>=============>>===========>>============>>==============>>============>>============>>=============>>===============>> *********
                         awaiting         ^                                   java avg wait time   ^^             ^              ^           *       *
                        for result        |                                        {31:00.00}           |\- send       \- receive     \- send     *       *
                          {29:00.00}           |                               /----- {32,10} -------/                                         * redis *
             /-------- {30,11} -------/                     receive -\|                 /- send        /- receive                        *       *
             v                                                           vv                 v              v                                 *       *
             [=====<<===============<<==============<<============<<===============<<==============<<=============<<======================<< *********
% total      |<-- {33:00.00} ->|<----- {34:00.00} ----->|<-- {35:00.00} ->|<--- {36:00.00} -->|<---- {37:00.00} ---->|<--- {38:00.00} -->|<--- {39:00.00} --...
% of step    |            |<----- {40:00.00} ----->|<-- {41:00.00} ->|<--- {42:00.00} -->|                 |              |
% of read    |<-- {43:00.00} ->|<----- {44:00.00} ----->|<-- {45:00.00} ->|<--- {46:00.00} -->|<---- {47:00.00} ---->|<--- {48:00.00} -->|
duration     |<- {49,6} ->|<---- {50,6} ----->|<- {51,6} ->|<-- {52,6} -->|<--- {53,6} ---->|<-- {54,6} -->|<-- {55,6} --...
             |            |                   |            |              |                 |              |
        /--------\        /-------------------+------------+--------------\     *******     /--------------\
        | client |        | set future result |   parse    | buffer write |     *     *     |   babushka   |
        \--------/        +-------------------+------------+--------------+     * UDS *     \--------------/
                          |                     netty                     |     *     *
                          \-----------------------------------------------/     *******
""";

        public static void Main(string[] args)
		{
			var lines = File.ReadAllLines(args.Last());

			int pos = 0;

			while (true)
			{
				(var parsed, pos) = ParseTestData(lines, pos);
				if (parsed == null) break;

				var data = parsed.Value;

				var write_duration = data.write_build_command + data.write_submit_command + data.write_handling + data.write_serialize + data.write_buffer + data.write_submit + data.java2rust_times.Sum() + data.rust_request_times.Sum();
				var read_duration = data.read_get_future_res + data.read_set_future + data.read_parse + data.read_buffer + data.rust2java_times.Sum() + data.rust_response_times.Sum();

				Console.WriteLine($"\n\n========= {data.name} ======== \n\n total time {data.total:#,###} for {data.reads} iterations\n write time {write_duration:#,###}\n read time  {read_duration:#,###}\n\n");

				var report = string.Format(report_template,
					data.write_build_command * 100.0 / data.total,
					data.write_submit_command * 100.0 / data.total,
					data.write_handling * 100.0 / data.total,
					data.write_serialize * 100.0 / data.total,
					data.write_buffer * 100.0 / data.total,
					data.write_submit * 100.0 / data.total,
					data.java2rust_times.Sum() * 100.0 / data.total,
					data.rust_request_times.Sum() * 100.0 / data.total,

					data.write_build_command * 100.0 / (data.write_build_command + data.write_submit_command),
					data.write_submit_command * 100.0 / (data.write_build_command + data.write_submit_command),
					data.write_serialize * 100.0 / (data.write_serialize + data.write_buffer + data.write_submit),
					data.write_buffer * 100.0 / (data.write_serialize + data.write_buffer + data.write_submit),
					data.write_submit * 100.0 / (data.write_serialize + data.write_buffer + data.write_submit),

					data.write_build_command * 100.0 / write_duration,
					data.write_submit_command * 100.0 / write_duration,
					data.write_handling * 100.0 / write_duration,
					data.write_serialize * 100.0 / write_duration,
					data.write_buffer * 100.0 / write_duration,
					data.write_submit * 100.0 / write_duration,
					data.java2rust_times.Sum() * 100.0 / write_duration,
					data.rust_request_times.Sum() * 100.0 / write_duration,

					data.write_build_command / data.writes,
					data.write_submit_command / data.writes,
					data.write_handling / data.writes,
					data.write_serialize / data.writes,
					data.write_buffer / data.writes,
					data.write_submit / data.writes,
					data.java2rust_times.Average(),
					data.rust_request_times.Average(),

					data.waiting * 100.0 / data.total,
					data.waiting,
					data.avg_resp_time * data.reads * 100.0 / data.total,
					data.avg_resp_time * data.reads,

					data.read_get_future_res * 100.0 / data.total,
					data.read_set_future * 100.0 / data.total,
					data.read_parse * 100.0 / data.total,
					data.read_buffer * 100.0 / data.total,
					data.rust2java_times.Sum() * 100.0 / data.total,
					data.rust_response_times.Sum() * 100.0 / data.total,
					data.redis_times.Sum() * 100.0 / data.total,

					data.read_set_future * 100.0 / (data.read_set_future + data.read_parse + data.read_buffer),
					data.read_parse * 100.0 / (data.read_set_future + data.read_parse + data.read_buffer),
					data.read_buffer * 100.0 / (data.read_set_future + data.read_parse + data.read_buffer),

					data.read_get_future_res * 100.0 / read_duration,
					data.read_set_future * 100.0 / read_duration,
					data.read_parse * 100.0 / read_duration,
					data.read_buffer * 100.0 / read_duration,
					data.rust2java_times.Sum() * 100.0 / read_duration,
					data.rust_response_times.Sum() * 100.0 / read_duration,

					data.read_get_future_res / data.reads,
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
